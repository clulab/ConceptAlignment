package org.clulab.alignment.webapp.indexer

import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexerApp
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.DatamartScraper
import org.clulab.alignment.scraper.ScraperApp
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.webapp.utils.StatusHolder

import scala.concurrent.Future

class IndexMessage(val message: String) // This will just be "finished"

trait IndexReceiver {
  def receive(indexSender: IndexSender, indexMessage: IndexMessage)
}

trait IndexSender {
}

class IndexCallback(indexReceiver: IndexReceiver, indexMessage: IndexMessage) {
  def callback(indexSender: IndexSender): Unit = indexReceiver.receive(indexSender, indexMessage)
}

class Indexer(indexerLocations: IndexerLocations, callback: Indexer.IndexCallbackType = Indexer.muteIndexCallback) extends IndexSender {

  val statusHolder: StatusHolder[IndexerStatus] = new StatusHolder[IndexerStatus](logger, IndexerIdling)

  val scraperApp = new ScraperApp(indexerLocations.scraperLocations)
  // This one can be really slow, so loading might require a future.
  // The w2v could be gotten from the searcher and reused when it is ready.
  // Is it able to update its location? or take w2v in constructor?
  val hnswlibIndexerApp = new HnswlibIndexerApp(indexerLocations.hnswlibLocations)
  val luceneIndexerApp = new LuceneIndexerApp(indexerLocations.luceneLocations)

  protected var runFutureOpt: Option[Future[DatamartIndex.Index]] = None

  def run(): Unit = run(ScraperApp.getScrapers)

  def run(supermaasUrl: String): Unit = run(ScraperApp.getScrapers(supermaasUrl))

  def run(scrapers: Seq[DatamartScraper]): Unit = {
//    runFutureOpt = Some(Future {
      indexerLocations.mkdirs()
      scraperApp.run(scrapers)
      val datamartIndex = hnswlibIndexerApp.run()
      luceneIndexerApp.run()
      callback(this)
      datamartIndex
//    })
  }
}

object Indexer {
  type IndexCallbackType = IndexSender => Unit

  val muteIndexCallback: IndexCallbackType = (indexSender: IndexSender) => ()
}
