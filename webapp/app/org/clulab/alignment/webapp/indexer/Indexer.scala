package org.clulab.alignment.webapp.indexer

import javax.inject.Inject
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexerApp
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.ScraperApp
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.webapp.utils.AutoLocations
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

trait IndexerTrait {
  def run(indexReceiverOpt: Option[IndexReceiver]): Unit
}

class IndexerApps(indexerLocations: IndexerLocations) {
  val scraperApp = new ScraperApp(indexerLocations.scraperLocations)
  // Try to recover the vectors before moving this.
  val hnswlibIndexerApp = new HnswlibIndexerApp(indexerLocations.hnswlibLocations)
  val luceneIndexerApp = new LuceneIndexerApp(indexerLocations.luceneLocations)
}

class Indexer(indexerLocations: IndexerLocations) extends IndexerTrait with IndexSender {
  import scala.concurrent.ExecutionContext.Implicits.global

  val statusHolder: StatusHolder[IndexerStatus] = new StatusHolder[IndexerStatus](logger, IndexerStatus.Loading)
  protected val loadingFuture = Future[IndexerApps] {
    val result = new IndexerApps(indexerLocations)
    statusHolder.set(IndexerStatus.Idling)
    result
  }
  val supermaasUrlOpt = Option(System.getenv(Indexer.supermaasUrlKey))
  val scrapers = supermaasUrlOpt
      .map { supermaasUrl =>
        ScraperApp.getScrapers(supermaasUrl)
      }
      .getOrElse(ScraperApp.getScrapers)
  protected var indexingFutureOpt: Option[Future[DatamartIndex.Index]] = None

  def getStatus: IndexerStatus = statusHolder.get

  // This does need a callback, at least a receiver, possibly None which is default.
  // We're not waiting for it.
  // Add the receiver or callback here
  def run(indexReceiverOpt: Option[IndexReceiver]): Unit = {
    statusHolder.set(IndexerStatus.Indexing)
    indexingFutureOpt = Some(
      loadingFuture.map { indexerApps =>
        indexerLocations.mkdirs()
        indexerApps.scraperApp.run(scrapers)
        val datamartIndex = indexerApps.hnswlibIndexerApp.run()
        indexerApps.luceneIndexerApp.run()
//        callback(this)
        datamartIndex // put this into the message so that it can be reused in the search.
      }
    )
  }
}

object Indexer {
  val supermaasUrlKey = "supermaas"
  type IndexCallbackType = IndexSender => Unit

  val muteIndexCallback: IndexCallbackType = (indexSender: IndexSender) => ()
}

class AutoIndexer @Inject()(autoLocations: AutoLocations)
    extends Indexer(new IndexerLocations(autoLocations.index + 1, autoLocations.baseDir, autoLocations.baseFile))
