package org.clulab.alignment.webapp.searcher

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.{CompositionalOntologyMapper, CompositionalOntologyToDatamarts, CompositionalOntologyToDocuments, FlatOntologyMapper, SingleKnnApp, SingleKnnAppTrait}

import java.util.concurrent.TimeUnit
import javax.inject._
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier
import org.clulab.alignment.data.ontology.FlatOntologyIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.FlatOntologyAlignmentItem
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.webapp.grounder.DojoDocument
import org.clulab.alignment.webapp.grounder.FlatGroundings
import org.clulab.alignment.webapp.grounder.SingleGrounding
import org.clulab.alignment.webapp.utils.AutoLocations
import org.clulab.alignment.webapp.utils.StatusHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

// The reason for the trait is that some things want to run straight on the
// SingleKnnApp rather than on the Searcher.  This keeps them compatible.
class Searcher(val searcherLocations: SearcherLocations, datamartIndexOpt: Option[DatamartIndex.Index] = None,
    var gloveIndexOpt: Option[GloveIndex.Index] = None,
    var flatOntologyMapperOpt: Option[FlatOntologyMapper] = None,
    var compositionalOntologyMapperOpt: Option[CompositionalOntologyMapper] = None)
    extends SingleKnnAppTrait {
  import scala.concurrent.ExecutionContext.Implicits.global

  def isReady: Boolean = flatOntologyMapperOpt.nonEmpty && compositionalOntologyMapperOpt.nonEmpty

  val statusHolder: StatusHolder[SearcherStatus] = new StatusHolder[SearcherStatus](getClass.getSimpleName, logger, SearcherStatus.Loading)
  val index: Int = searcherLocations.index
  protected val loadingFuture: Future[SingleKnnApp] = Future {
    try {
      val singleKnnApp = new SingleKnnApp(searcherLocations, datamartIndexOpt, gloveIndexOpt)
      gloveIndexOpt = Some(singleKnnApp.gloveIndex)
      val ontologyIndex = FlatOntologyIndex.load(searcherLocations.ontologyFilename)
      flatOntologyMapperOpt = Some(new FlatOntologyMapper(singleKnnApp.datamartIndex, ontologyIndex))
      val conceptIndex = FlatOntologyIndex.load(searcherLocations.conceptFilename)
      val processIndex = FlatOntologyIndex.load(searcherLocations.processFilename)
      val propertyIndex = FlatOntologyIndex.load(searcherLocations.propertyFilename)
      compositionalOntologyMapperOpt = Some(new CompositionalOntologyMapper(singleKnnApp.datamartIndex, conceptIndex, processIndex, propertyIndex))
      statusHolder.set(SearcherStatus.Waiting)
      singleKnnApp
    }
    catch {
      case throwable: Throwable =>
        Searcher.logger.error(s"""Exception caught loading searcher on index $index""", throwable)
        statusHolder.set(SearcherStatus.Failing)
        throw throwable // This will cause a crash.  Return a NullSearcher instead.
    }
  }

  def getStatus: SearcherStatus = statusHolder.get

  // This doesn't need a callback because we'll wait for it.
  override def run(queryString: String, maxHits: Int, thresholdOpt: Option[Float]): Seq[(DatamartDocument, Float)] = {
    val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
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
    val result = Await.result(searchingFuture, maxWaitTime)
    result
  }

  def getDatamartDocuments(datamartIdentifiers: Seq[DatamartIdentifier]): Seq[DatamartDocument] = {
    val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      singleKnnApp.getDatamartDocumentsFromIds(datamartIdentifiers)
    }
    val result = Await.result(searchingFuture, maxWaitTime)
    result
  }

  def runOld(homeId: CompositionalOntologyIdentifier, awayIds: Array[CompositionalOntologyIdentifier], maxHits: Int, thresholdOpt: Option[Float]): CompositionalOntologyToDatamarts = {
    val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
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
    val datamartsResult: CompositionalOntologyToDatamarts = Await.result(searchingFuture, maxWaitTime)

    datamartsResult
  }


  def run(homeId: CompositionalOntologyIdentifier, awayIds: Array[CompositionalOntologyIdentifier], maxHits: Int, thresholdOpt: Option[Float]): CompositionalOntologyToDocuments = {
    val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
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
    val datamartsResult: CompositionalOntologyToDatamarts = Await.result(searchingFuture, maxWaitTime)
    val datamartIdentifiers = datamartsResult.dstResults.map(_._1)
    val datamartDocuments = getDatamartDocuments(datamartIdentifiers)
    val datamartDocumentsAndFloats = datamartsResult.dstResults.zip(datamartDocuments).map { case (datamartIdentifierAndFloat, datamartDocument) =>
      (datamartDocument, datamartIdentifierAndFloat._2)
    }
    val documentsResult: CompositionalOntologyToDocuments = CompositionalOntologyToDocuments(datamartsResult.srcId, datamartDocumentsAndFloats)

    documentsResult
  }

  def run(dojoDocument: DojoDocument, maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): String = {
    val maxWaitTime: FiniteDuration = Duration(300, TimeUnit.SECONDS)
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
    val result: String = Await.result(searchingFuture, maxWaitTime)
    result
  }

  def next(index: Int, datamartIndex: DatamartIndex.Index): Searcher = {
    val nextSearcherLocations = new SearcherLocations(index, searcherLocations.baseDir, searcherLocations.baseFile)
    val nextSearcher = new Searcher(nextSearcherLocations, Some(datamartIndex), gloveIndexOpt, flatOntologyMapperOpt, compositionalOntologyMapperOpt)

    nextSearcher
  }

  def close(): Unit = {
    // Change state to closing
  }
}

object Searcher {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}

class AutoSearcher @Inject()(autoLocations: AutoLocations)
    extends Searcher(new SearcherLocations(autoLocations.index, autoLocations.baseDir, autoLocations.baseFile))
