package org.clulab.alignment.webapp.indexer

import javax.inject.Inject
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexerApp
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.DatamartScraper
import org.clulab.alignment.scraper.ScraperApp
import org.clulab.alignment.webapp.controllers.v1.HomeController.logger
import org.clulab.alignment.webapp.utils.AutoLocations
import org.clulab.alignment.webapp.utils.StatusHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class IndexMessage(val index: Int, val datamartIndex: DatamartIndex.Index)

trait IndexReceiver {
  def receive(indexSender: IndexSender, indexMessage: IndexMessage)
}

trait IndexSender {
}

class IndexCallback(indexReceiver: IndexReceiver, indexMessage: IndexMessage) {
  def callback(indexSender: IndexSender): Unit = indexReceiver.receive(indexSender, indexMessage)
}

trait IndexerTrait {
  val index: Int

  def getStatus: IndexerStatus

  def next(indexReceiverOpt: Option[IndexReceiver]): IndexerTrait

  def close(): Unit
}

class IndexerApps(indexerLocations: IndexerLocations) {
  val scraperApp = new ScraperApp(indexerLocations.scraperLocations)
  val hnswlibIndexerApp = new HnswlibIndexerApp(indexerLocations.hnswlibLocations)
  val luceneIndexerApp = new LuceneIndexerApp(indexerLocations.luceneLocations)
}

class Indexer(indexerLocations: IndexerLocations, scrapers: Seq[DatamartScraper], indexerApps: IndexerApps, indexReceiverOpt: Option[IndexReceiver]) extends IndexerTrait with IndexSender {
  import scala.concurrent.ExecutionContext.Implicits.global

  val index: Int = indexerLocations.index
  val statusHolder: StatusHolder[IndexerStatus] = new StatusHolder[IndexerStatus](getClass.getSimpleName, Indexer.logger, IndexerStatus.Indexing)
  val indexingFuture: Future[Unit] = Future {
    try {
      indexerLocations.mkdirs()
      indexerApps.scraperApp.run(scrapers)
      val datamartIndex = indexerApps.hnswlibIndexerApp.run()
      indexerApps.luceneIndexerApp.run()
      indexReceiverOpt.foreach { indexReceiver =>
        val indexMessage = new IndexMessage(index, datamartIndex)
        indexReceiver.receive(this, indexMessage)
      }
      statusHolder.set(IndexerStatus.Idling)
    }
    catch {
      case throwable: Throwable =>
        Indexer.logger.error(s"""Exception caught indexing $index""", throwable)
        statusHolder.set(IndexerStatus.Failing)
        throw throwable
    }
  }

  def getStatus: IndexerStatus = statusHolder.get

  def next(indexReceiverOpt: Option[IndexReceiver]): Indexer = {
    require(indexingFuture.isCompleted)
    val nextLocation = new IndexerLocations(indexerLocations.index + 1, indexerLocations.baseDir, indexerLocations.baseFile)
    val result = new Indexer(nextLocation, scrapers, indexerApps, indexReceiverOpt)
    close()
    result
  }

  def close(): Unit = {
    // change state to closing
    // make sure all files are closed in apps
    // delete files?
  }
}

object Indexer {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  type IndexCallbackType = IndexSender => Unit

  val muteIndexCallback: IndexCallbackType = (indexSender: IndexSender) => ()
}

class AutoIndexer @Inject()(autoLocations: AutoLocations) extends IndexerTrait {
  import scala.concurrent.ExecutionContext.Implicits.global

  val index: Int = autoLocations.index
  val indexerLocations = new IndexerLocations(index, autoLocations.baseDir, autoLocations.baseFile)
  val statusHolder: StatusHolder[IndexerStatus] = new StatusHolder[IndexerStatus](getClass.getSimpleName, logger, IndexerStatus.Loading)
  val scrapers: Seq[DatamartScraper] = ScraperApp.getScrapers
  val loadingFuture: Future[IndexerApps] = Future[IndexerApps] {
    try {
      val result = new IndexerApps(indexerLocations)
      statusHolder.set(IndexerStatus.Idling)
      result
    }
    catch {
      case throwable: Throwable =>
        Indexer.logger.error(s"""Exception caught loading indexer on index $index""", throwable)
        statusHolder.set(IndexerStatus.Crashing)
        throw throwable
    }
  }

  def getStatus: IndexerStatus = statusHolder.get

  def next(indexReceiverOpt: Option[IndexReceiver]): IndexerTrait = {
    require(loadingFuture.isCompleted)
    val nextLocation = new IndexerLocations(indexerLocations.index + 1, indexerLocations.baseDir, indexerLocations.baseFile)
    val result = new Indexer(nextLocation, scrapers, new IndexerApps(nextLocation), indexReceiverOpt)
    close()

    result
  }

  // There wouldn't be any files to delete for this one.
  def close(): Unit = ()
}
