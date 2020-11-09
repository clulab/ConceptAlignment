package org.clulab.alignment.webapp.indexer

import javax.inject.Inject
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexerApp
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.ScraperApp
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.webapp.utils.AutoLocations
import org.clulab.alignment.webapp.utils.StatusHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class IndexMessage(val indexer: Indexer, val datamartIndex: DatamartIndex.Index)

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
  val hnswlibIndexerApp = new HnswlibIndexerApp(indexerLocations.hnswlibLocations)
  val luceneIndexerApp = new LuceneIndexerApp(indexerLocations.luceneLocations)
}

// have options[datamartIndex: DatamartIndex.Index]
// also index for glove as well, default to Some[(index1, index2)]
// and use other constructor
class Indexer(indexerLocations: IndexerLocations) extends IndexerTrait with IndexSender {
  import scala.concurrent.ExecutionContext.Implicits.global

  val statusHolder: StatusHolder[IndexerStatus] = new StatusHolder[IndexerStatus](getClass.getSimpleName, logger, IndexerStatus.Loading)
  val index: Int = indexerLocations.index
  protected val loadingFuture: Future[IndexerApps] = Future[IndexerApps] {
    try {
      val result = new IndexerApps(indexerLocations)
      statusHolder.set(IndexerStatus.Idling)
      result
    }
    catch {
      case throwable: Throwable =>
        Indexer.logger.error(s"""Exception caught loading indexer on index $index""", throwable)
        statusHolder.set(IndexerStatus.Failing)
        throw throwable // This will cause a crash.  Return a NullIndexer instead.
    }
  }
  val supermaasUrlOpt = Option(System.getenv(Indexer.supermaasUrlKey))
  val scrapers = supermaasUrlOpt
      .map { supermaasUrl =>
        ScraperApp.getScrapers(supermaasUrl)
      }
      .getOrElse(ScraperApp.getScrapers)
  // This is an attempt to make sure the future isn't garbage collected.
  protected var indexingFutureOpt: Option[Future[Unit]] = None

  def getStatus: IndexerStatus = statusHolder.get

  def run(indexReceiverOpt: Option[IndexReceiver]): Unit = {
    // This might be a race condition.
    statusHolder.set(IndexerStatus.Indexing)
    indexingFutureOpt = Some(
      loadingFuture.map { indexerApps =>
        try {
          indexerLocations.mkdirs()
          indexerApps.scraperApp.run(scrapers)
          val datamartIndex = indexerApps.hnswlibIndexerApp.run()
          indexerApps.luceneIndexerApp.run()
          indexReceiverOpt.map { indexReceiver =>
            val indexMessage = new IndexMessage(this, datamartIndex)
            indexReceiver.receive(this, indexMessage)
          }
        }
        catch {
          case throwable: Throwable =>
            Indexer.logger.error(s"""Exception caught indexing $index""", throwable)
            statusHolder.set(IndexerStatus.Failing)
            // On fail, return a NullIndexer marked failing but with an increased index.
            // Searches will continue on the previous index?
        }
      }
    )
  }

  def next: Indexer = {
    val nextLocation = new IndexerLocations(indexerLocations.index + 1, indexerLocations.baseDir, indexerLocations.baseFile)
    new Indexer(nextLocation)
  }

  def close(): Unit = {
    // change state to closing
    // make sure all files are closed in apps
    // delete files?
  }
}

object Indexer {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val supermaasUrlKey = "supermaas"
  type IndexCallbackType = IndexSender => Unit

  val muteIndexCallback: IndexCallbackType = (indexSender: IndexSender) => ()
}

class AutoIndexer @Inject()(autoLocations: AutoLocations)
    extends Indexer(new IndexerLocations(autoLocations.index + 1, autoLocations.baseDir, autoLocations.baseFile))
