package org.clulab.alignment.controllers.utils

import java.util.concurrent.TimeUnit

import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.SingleKnnAppTrait
import org.clulab.alignment.controllers.v1.HomeController.logger
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

class SingleKnnAppFuture extends SingleKnnAppTrait {

  println("********** the knn started up ***********")
  import scala.concurrent.ExecutionContext.Implicits.global

  val statusHolder: StatusHolder = new StatusHolder(logger, Busy)

  protected val singleKnnAppFuture: Future[SingleKnnApp] = Future {
    val result = new SingleKnnApp()
    statusHolder.set(Ready)
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

object SingleKnnAppFuture {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
