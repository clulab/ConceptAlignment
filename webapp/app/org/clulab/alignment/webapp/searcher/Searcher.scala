package org.clulab.alignment.webapp

import javax.inject._
import java.util.concurrent.TimeUnit

import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.SingleKnnAppTrait
import org.clulab.alignment.searcher.knn.KnnLocations
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

// The reason for the trait is that some things want to run straight on the
// SingleKnnApp rather than on the Future.  This keeps them compatible.
class Searcher(val locations: KnnLocations) extends SingleKnnAppTrait {
  import scala.concurrent.ExecutionContext.Implicits.global

  val statusHolder: StatusHolder[SearcherStatus] = new StatusHolder[SearcherStatus](logger, SearcherLoading)

  protected val singleKnnAppFuture: Future[SingleKnnApp] = Future {
    val result = new SingleKnnApp() // This can take a very long time.
    statusHolder.set(SearcherIdling)
    result
  }

  override def run(queryString: String, maxHits: Int): Seq[(DatamartDocument, Float)] = {
    val maxWaitTime: FiniteDuration = Duration(200, TimeUnit.SECONDS)
    val runFuture = singleKnnAppFuture.map { singleKnnApp =>
      singleKnnApp.run(queryString, maxHits)
    }

    Await.result(runFuture, maxWaitTime)
  }
}

object Searcher {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}

class AutoSearcher @Inject()(locations: AutoKnnLocations) extends Searcher(locations)
