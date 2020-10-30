package org.clulab.alignment.webapp.searcher

import java.util.concurrent.TimeUnit

import javax.inject._
import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.SingleKnnAppTrait
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
class Searcher(val locations: SearcherLocations) extends SingleKnnAppTrait {
  import scala.concurrent.ExecutionContext.Implicits.global

  val statusHolder: StatusHolder[SearcherStatus] = new StatusHolder[SearcherStatus](logger, SearcherStatus.Loading)
  protected val loadingFuture: Future[SingleKnnApp] = Future {
    val result = new SingleKnnApp() // This can take a very long time.
    statusHolder.set(SearcherStatus.Idling)
    result
  }

  def getStatus: SearcherStatus = statusHolder.get

  // This doesn't need a callback because we'll wait for it.
  override def run(queryString: String, maxHits: Int): Seq[(DatamartDocument, Float)] = {
    val maxWaitTime: FiniteDuration = Duration(200, TimeUnit.SECONDS)
    val searchingFuture = loadingFuture.map { singleKnnApp =>
      singleKnnApp.run(queryString, maxHits)
    }

    Await.result(searchingFuture, maxWaitTime)
  }
}

object Searcher {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}

class AutoSearcher @Inject()(autoLocations: AutoLocations)
    extends Searcher(new SearcherLocations(autoLocations.index, autoLocations.baseDir, autoLocations.baseFile))
