package org.clulab.alignment.webapp.searcher

import org.clulab.alignment.CompositionalOntologyMapper

import java.util.concurrent.TimeUnit
import javax.inject._
import org.clulab.alignment.FlatOntologyMapper
import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.SingleKnnAppTrait
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
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
