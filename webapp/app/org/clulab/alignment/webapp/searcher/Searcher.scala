package org.clulab.alignment.webapp.searcher

import org.clulab.alignment.{CompositionalOntologyMapper, CompositionalOntologyToDatamarts, CompositionalOntologyToDocuments, FlatOntologyMapper, SingleKnnApp, SingleKnnAppTrait}
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier
import org.clulab.alignment.exception.ExternalException
import org.clulab.alignment.exception.InternalException
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexer
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.webapp.grounder.DojoDocument
import org.clulab.alignment.webapp.utils.AutoLocations
import org.clulab.alignment.webapp.utils.StatusHolder
import org.clulab.utils.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import javax.inject._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

// The reason for the trait is that some things want to run straight on the
// SingleKnnApp rather than on the Searcher.  This keeps them compatible.
class Searcher(val searcherLocations: SearcherLocations, datamartIndexOpt: Option[DatamartIndex.Index] = None,
    var gloveIndexOpt: Option[GloveIndex.Index] = None,
    var flatOntologyMapperOpt: Option[FlatOntologyMapper] = None,
    var compositionalOntologyMapperOpt: Option[CompositionalOntologyMapper] = None,
    var dynamicCompositionalOntologyMapperOpt: Option[mutable.Map[String, CompositionalOntologyMapper]] = None)
    extends SingleKnnAppTrait {
  import scala.concurrent.ExecutionContext.Implicits.global

  def isReady: Boolean = flatOntologyMapperOpt.nonEmpty && compositionalOntologyMapperOpt.nonEmpty

  val statusHolder: StatusHolder[SearcherStatus] = new StatusHolder[SearcherStatus](getClass.getSimpleName, Searcher.logger, SearcherStatus.Loading)
  val index: Int = searcherLocations.index
  var addingOntology: Boolean = false
  protected val loadingFuture: Future[SingleKnnApp] = Future {
    try {
      // Reuse the glove index when possible.
      val singleKnnApp = new SingleKnnApp(searcherLocations, datamartIndexOpt, gloveIndexOpt)
      val datamartIndex = singleKnnApp.datamartIndex
      val isDatamartEmpty = datamartIndex.isEmpty
      gloveIndexOpt = Some(singleKnnApp.gloveIndex)
      // Reuse the ontologyIndex when possible.
      flatOntologyMapperOpt = Some(FlatOntologyMapper(flatOntologyMapperOpt, datamartIndex, searcherLocations.ontologyFilename))
      // Reuse the compositional indexes when possible.
      compositionalOntologyMapperOpt = Some(CompositionalOntologyMapper(compositionalOntologyMapperOpt, datamartIndex,
          searcherLocations.conceptFilename, searcherLocations.processFilename, searcherLocations.propertyFilename))
      // Look for new ontologies _or_ find the existing ones and copy them over with a new datamartIndex.

      val dynamicCompositionalOntologyMapper = mkOrCopyDynamicOntologyMap(dynamicCompositionalOntologyMapperOpt, datamartIndex)
      dynamicCompositionalOntologyMapperOpt = Some(dynamicCompositionalOntologyMapper)

      if (isDatamartEmpty) statusHolder.set(SearcherStatus.Wanting)
      else statusHolder.set(SearcherStatus.Waiting)
      singleKnnApp
    }
    catch {
      case throwable: Throwable =>
        Searcher.logger.error(s"""Exception caught loading searcher on index $index""", throwable)
        statusHolder.set(SearcherStatus.Failing)
        throw throwable // This will cause a crash.  Return a NullSearcher instead.
    }
  }

  def addOntology(ontologyId: String, ontology: String): Unit = {
    val addingFuture = loadingFuture.map { singleKnnApp =>
      // We don't know what ontologies are pre-loaded until loading is complete.
      Searcher.synchronized {
        if (addingOntology)
          throw new ExternalException("Please wait until the previous ontology has been loaded before adding another.")
        addingOntology = true
        // This needs to be synchronized in case it is called twice.
        // It is synchronized against the object because the global filesystem is changing.
        // It may be that this is called on a Searcher that has become obsolete.
        val dynamicCompositionalOntologyMapper = dynamicCompositionalOntologyMapperOpt.get
        if (dynamicCompositionalOntologyMapper.contains(ontologyId))
          throw new ExternalException(s"Ontology `$ontologyId` already exists.")
        val filename = CompositionalOntologyMapper.mkOntologyFilename(searcherLocations.baseDir, ontologyId)
        FileUtils.printWriterFromFile(filename).autoClose { printWriter =>
          printWriter.print(ontology)
        }

        {
          val indexer = new HnswlibIndexer()
          val conceptFilename = CompositionalOntologyMapper.mkIndexFilename(searcherLocations.baseDir, searcherLocations.conceptFilename, ontologyId)
          val processFilename = CompositionalOntologyMapper.mkIndexFilename(searcherLocations.baseDir, searcherLocations.processFilename, ontologyId)
          val propertyFilename = CompositionalOntologyMapper.mkIndexFilename(searcherLocations.baseDir, searcherLocations.propertyFilename, ontologyId)

          indexer.indexCompositionalOntology(conceptFilename, processFilename, propertyFilename, Some(filename), Some(ontologyId))
        }
        val datamartIndex = singleKnnApp.datamartIndex
        val compositionalOntologyMapper = CompositionalOntologyMapper(ontologyId, datamartIndex, searcherLocations.baseDir,
          searcherLocations.conceptFilename, searcherLocations.processFilename, searcherLocations.propertyFilename)

        synchronized {
          dynamicCompositionalOntologyMapper += ontologyId -> compositionalOntologyMapper
        }
        addingOntology = false
      }
    }

    // What happens when this doesn't finish in time?  A TimeoutException is thrown here, but
    // the future should run to completion.  Because of the exception, no value is returned,
    // so the called should be able to do without it.
    Await.result(addingFuture, Searcher.maxWaitTime * 10)
  }

  def getOntologyIdOpts: Option[collection.Set[String]] = synchronized {
    dynamicCompositionalOntologyMapperOpt.map(_.keySet)
  }

  // This is only called during loading.
  def mkOrCopyDynamicOntologyMap(dynamicCompositionalOntologyMapperOpt: Option[mutable.Map[String, CompositionalOntologyMapper]],
      datamartIndex: DatamartIndex.Index): mutable.Map[String, CompositionalOntologyMapper] = {
    dynamicCompositionalOntologyMapperOpt
        .map { dynamicCompositionalOntologyMapper =>
          val immutableMap = dynamicCompositionalOntologyMapper.mapValues { compositionalOntologyMapper =>
            CompositionalOntologyMapper(compositionalOntologyMapper, datamartIndex)
          }

          mutable.Map(immutableMap.toSeq: _*)
        }
        .getOrElse {
          val ontologyIds = FileUtils.findFiles(searcherLocations.baseDir, ".ont").map { file =>
            StringUtils.beforeLast(file.getName, '.')
          }
          val pairs = ontologyIds.map { ontologyId =>
            ontologyId -> CompositionalOntologyMapper(ontologyId, datamartIndex, searcherLocations.baseDir,
              searcherLocations.conceptFilename, searcherLocations.processFilename, searcherLocations.propertyFilename)
          }
          val alternative: mutable.Map[String, CompositionalOntologyMapper] = mutable.Map(pairs: _*)

          alternative
        }
  }

  def getStatus: SearcherStatus = statusHolder.get

  def runOld(queryString: String, maxHits: Int, thresholdOpt: Option[Float]): Seq[(DatamartIdentifier, Float)] = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      try {
        singleKnnApp.runOld(queryString, maxHits, thresholdOpt)
      }
      catch {
        case throwable: Throwable =>
          Searcher.logger.error(s"""Exception caught searching for $maxHits hits of "$queryString" on index $index""", throwable)
          statusHolder.set(SearcherStatus.Failing)
          Seq.empty
      }
    }
    val result = Await.result(searchingFuture, Searcher.maxWaitTime)
    result
  }

  // This doesn't need a callback because we'll wait for it.
  override def run(queryString: String, maxHits: Int, thresholdOpt: Option[Float]): Seq[(DatamartDocument, Float)] = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      try {
        singleKnnApp.run(queryString, maxHits, thresholdOpt)
      }
      catch {
        case throwable: Throwable =>
          Searcher.logger.error(s"""Exception caught searching for $maxHits hits of "$queryString" on index $index""", throwable)
          statusHolder.set(SearcherStatus.Failing)
          Seq.empty
      }
    }
    val result = Await.result(searchingFuture, Searcher.maxWaitTime)
    result
  }

  def getDatamartDocuments(datamartIdentifiers: Seq[DatamartIdentifier]): Seq[DatamartDocument] = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      singleKnnApp.getDatamartDocumentsFromIds(datamartIdentifiers)
    }
    val result = Await.result(searchingFuture, Searcher.maxWaitTime)
    result
  }

  def runOld(homeId: CompositionalOntologyIdentifier, awayIds: Array[CompositionalOntologyIdentifier], maxHits: Int, thresholdOpt: Option[Float]): CompositionalOntologyToDatamarts = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      try {
        compositionalOntologyMapperOpt.get.ontologyItemToDatamartMapping(homeId, awayIds, Some(maxHits), thresholdOpt)
      }
      catch {
        case throwable: Throwable =>
          Searcher.logger.error(s"""Exception caught compositionally searching for $maxHits hits of "$homeId" on index $index""", throwable)
          throw throwable // statusHolder.set(SearcherStatus.Failing)
        // new CompositionalOntologyToDatamarts(homeId, Seq.empty)
      }
    }
    val result: CompositionalOntologyToDatamarts = Await.result(searchingFuture, Searcher.maxWaitTime)

    result
  }


  def run(homeId: CompositionalOntologyIdentifier, awayIds: Array[CompositionalOntologyIdentifier], maxHits: Int, thresholdOpt: Option[Float]): CompositionalOntologyToDocuments = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      try {
        compositionalOntologyMapperOpt.get.ontologyItemToDatamartMapping(homeId, awayIds, Some(maxHits), thresholdOpt)
      }
      catch {
        case throwable: Throwable =>
          Searcher.logger.error(s"""Exception caught compositionally searching for $maxHits hits of "$homeId" on index $index""", throwable)
          throw throwable // statusHolder.set(SearcherStatus.Failing)
          // new CompositionalOntologyToDatamarts(homeId, Seq.empty)
      }
    }
    val datamartsResult: CompositionalOntologyToDatamarts = Await.result(searchingFuture, Searcher.maxWaitTime)
    val datamartIdentifiers = datamartsResult.dstResults.map(_._1)
    val datamartDocuments = getDatamartDocuments(datamartIdentifiers)
    val datamartDocumentsAndFloats = datamartsResult.dstResults.zip(datamartDocuments).map { case (datamartIdentifierAndFloat, datamartDocument) =>
      (datamartDocument, datamartIdentifierAndFloat._2)
    }
    val documentsResult: CompositionalOntologyToDocuments = CompositionalOntologyToDocuments(datamartsResult.srcId, datamartDocumentsAndFloats)

    documentsResult
  }

  def getValidDatamartDocuments(geography: Seq[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Seq[DatamartIdentifier] = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      singleKnnApp.luceneSearcher.search(geography, periodGteOpt, periodLteOpt)
    }
    val validDatamartIdentifiers = Await.result(searchingFuture, Searcher.maxWaitTime)

    validDatamartIdentifiers
  }

  class Filter(geography: List[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]) {
    val validDatamartIdentifiersOpt =
        if (geography.nonEmpty || periodGteOpt.nonEmpty || periodLteOpt.nonEmpty)
          Some(getValidDatamartDocuments(geography, periodGteOpt, periodLteOpt).toSet)
        else
          None

    def isEmpty: Boolean = validDatamartIdentifiersOpt.isDefined && validDatamartIdentifiersOpt.get.isEmpty

    def filter(compositionalOntologyToDatamarts: CompositionalOntologyToDatamarts, maxHits: Int): CompositionalOntologyToDatamarts = {
      validDatamartIdentifiersOpt
          .map {validDatamartIdentifiers =>
            val filteredPairs = compositionalOntologyToDatamarts.dstResults.filter { case (datamartIdentifier, _) =>
              validDatamartIdentifiers(datamartIdentifier)
            }.take(maxHits)

            CompositionalOntologyToDatamarts(compositionalOntologyToDatamarts.srcId, filteredPairs)
          }
          .getOrElse {
            val filteredPairs = compositionalOntologyToDatamarts.dstResults.take(maxHits)

            CompositionalOntologyToDatamarts(compositionalOntologyToDatamarts.srcId, filteredPairs)
          }
    }
  }

  def toDocuments(compositionalOntologyToDatamarts: CompositionalOntologyToDatamarts): CompositionalOntologyToDocuments = {
    val datamartIdentifiers = compositionalOntologyToDatamarts.dstResults.map(_._1)
    val datamartDocuments = getDatamartDocuments(datamartIdentifiers)
    val datamartDocumentsAndFloats = compositionalOntologyToDatamarts.dstResults.zip(datamartDocuments).map { case (datamartIdentifierAndFloat, datamartDocument) =>
      (datamartDocument, datamartIdentifierAndFloat._2)
    }

    CompositionalOntologyToDocuments(compositionalOntologyToDatamarts.srcId, datamartDocumentsAndFloats)
  }

  def run2(filter: Filter, maxHits: Int, thresholdOpt: Option[Float], ontologyIdOpt: Option[String], compositionalSearchSpec: CompositionalSearchSpec): CompositionalOntologyToDocuments = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      val compositionalOntologyMapper = ontologyIdOpt
        .map { ontologyId =>
          synchronized {
            dynamicCompositionalOntologyMapperOpt.get.getOrElse(ontologyId, throw new ExternalException(s"The ontologyId '$ontologyId' is not available.  Please check its status."))
          }
        }
        .getOrElse(compositionalOntologyMapperOpt.get)
      val homeId = compositionalSearchSpec.homeId
      try {
        val contextOpt = compositionalSearchSpec.contextOpt
        Searcher.logger.trace(s"ContextOpt: $contextOpt")
        val contextVectorOpt = contextOpt.flatMap { context => singleKnnApp.getVectorOpt(context) }
        val awayIds = compositionalSearchSpec.awayIds

        compositionalOntologyMapper.ontologyItemToDatamartMappingWithContextOpt(contextVectorOpt, homeId, awayIds, None, thresholdOpt) // Skip maxHits here.
      }
      catch {
        case externalException: ExternalException => throw externalException
        case throwable: Throwable =>
          throw new InternalException(s"""Exception caught compositionally searching for $maxHits hits of "$homeId" on index $index""", throwable)
      }
    }
    val rawCompositionalOntologyToDatamarts = Await.result(searchingFuture, Searcher.maxWaitTime)
    val compositionalOntologyToDatamarts = filter.filter(rawCompositionalOntologyToDatamarts, maxHits)
    val compositionalOntologyToDocuments = toDocuments(compositionalOntologyToDatamarts)

    compositionalOntologyToDocuments
  }

  def run2(compositionalSearchSpec: CompositionalSearchSpec, maxHits: Int, thresholdOpt: Option[Float],
           ontologyIdOpt: Option[String], geography: List[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): CompositionalOntologyToDocuments = {
    val filter = new Filter(geography, periodGteOpt, periodLteOpt)

    if (filter.isEmpty)
      CompositionalOntologyToDocuments(compositionalSearchSpec.homeId, Seq.empty)
    else
      run2(filter, maxHits, thresholdOpt, ontologyIdOpt, compositionalSearchSpec)
  }

  def run2(filter: Filter, maxHits: Int, thresholdOpt: Option[Float],
           ontologyIdOpt: Option[String], compositionalSearchSpecs: Array[CompositionalSearchSpec]): Array[CompositionalOntologyToDocuments] = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      val compositionalOntologyMapper = ontologyIdOpt
        .map { ontologyId =>
          synchronized {
            dynamicCompositionalOntologyMapperOpt.get.getOrElse(ontologyId, throw new ExternalException(s"The ontologyId '$ontologyId' is not available.  Please check its status."))
          }
        }
        .getOrElse(compositionalOntologyMapperOpt.get)
      compositionalSearchSpecs.map { compositionalSearchSpec =>
        val homeId = compositionalSearchSpec.homeId
        try {
          val contextOpt = compositionalSearchSpec.contextOpt
          Searcher.logger.trace(s"ContextOpt: $contextOpt")
          val awayIds = compositionalSearchSpec.awayIds
          val contextVectorOpt = contextOpt.flatMap { context => singleKnnApp.getVectorOpt(context) }

          compositionalOntologyMapper.ontologyItemToDatamartMappingWithContextOpt(contextVectorOpt, homeId, awayIds, None, thresholdOpt) // Skip maxHits here.
        }
        catch {
          case throwable: Throwable =>
            // These errors will be suppressed in the hopes of returning something for every slot.
            CompositionalOntologyToDatamarts(homeId, Seq.empty)
            // throw new InternalException(s"""Exception caught compositionally searching for $maxHits hits of "$homeId" on index $index""", throwable)
        }
      }
    }

    val multipleRawCompositionalOntologyToDatamarts = Await.result(searchingFuture, Searcher.maxWaitTime)
    val multipleCompositionalOntologyToDatamarts = multipleRawCompositionalOntologyToDatamarts.map(filter.filter(_, maxHits))
    val multipleCompositionalOntologyToDocuments = multipleCompositionalOntologyToDatamarts.map(toDocuments)

    multipleCompositionalOntologyToDocuments
  }

  def run2(compositionalSearchSpecs: Array[CompositionalSearchSpec], maxHits: Int, thresholdOpt: Option[Float],
      ontologyIdOpt: Option[String], geography: List[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Array[CompositionalOntologyToDocuments] = {
    val filter = new Filter(geography, periodGteOpt, periodLteOpt)

    if (filter.isEmpty) {
      compositionalSearchSpecs.map { compositionalSearchSpec =>
        CompositionalOntologyToDocuments(compositionalSearchSpec.homeId, Seq.empty)
      }
    }
    else
      run2(filter, maxHits, thresholdOpt, ontologyIdOpt, compositionalSearchSpecs)
  }

  def run(dojoDocument: DojoDocument, maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): String = {
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      try {
        val groundedModel =
            if (!compositional) dojoDocument.groundFlat(singleKnnApp, flatOntologyMapperOpt.get, maxHits, thresholdOpt)
            else dojoDocument.groundComp(singleKnnApp, compositionalOntologyMapperOpt.get, maxHits, thresholdOpt)

        groundedModel.toJson
      }
      catch {
        case throwable: Throwable =>
          Searcher.logger.error(s"""Exception caught searching dojoDocument for $maxHits hits on index $index""", throwable)
          //statusHolder.set(SearcherStatus.Failing)
          //dojoDocument.toJson
          throw throwable
      }
    }
    val result: String = Await.result(searchingFuture, Searcher.maxWaitTime)
    result
  }

  def next(index: Int, datamartIndex: DatamartIndex.Index): Searcher = {
    val nextSearcherLocations = new SearcherLocations(index, searcherLocations.baseDir, searcherLocations.baseFile)
    val nextSearcher = new Searcher(nextSearcherLocations, Some(datamartIndex), gloveIndexOpt, flatOntologyMapperOpt,
        compositionalOntologyMapperOpt, dynamicCompositionalOntologyMapperOpt)

    nextSearcher
  }

  def close(): Unit = {
    loadingFuture.map { singleKnnApp =>
      singleKnnApp.luceneSearcher.close()
    }
  }
}

object Searcher {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
}

class AutoSearcher @Inject()(autoLocations: AutoLocations)
    extends Searcher(new SearcherLocations(autoLocations.index, autoLocations.baseDir, autoLocations.baseFile))

case class CompositionalSearchSpec(contextOpt: Option[String], homeId: CompositionalOntologyIdentifier, awayIds: Array[CompositionalOntologyIdentifier])
