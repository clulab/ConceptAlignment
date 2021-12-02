package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.utils.{Sourcer, TsvWriter}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.{HashSet => MutableHashSet}

class DojoFileScraper(filename: String) extends DojoScraper {

  def scrapeModels(tsvWriter: TsvWriter): Unit = {
  }

  def scrapeIndicators(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.foreach { line =>
        val jObj = ujson.read(line).obj
        val dojoDocument = new IndicatorDocument(jObj)
        val datasetId = dojoDocument.id

        DojoFileScraper.logger.info(s"Scraping DOJO datasetId $datasetId")
        dojoDocument.outputs.foreach { dojoOutput =>
          writeDojoRecord(dojoDocument, dojoOutput, tsvWriter, doubleIds, DojoFileScraper.logger)
        }
//        dojoDocument.qualifierOutputsOpt.foreach { dojoQualifierOutputs =>
//          dojoQualifierOutputs.foreach { dojoQualifierOutput =>
//            writeDojoRecord(dojoDocument, dojoQualifierOutput, tsvWriter, doubleIds)
//          }
//        }
      }
    }
  }

  def scrape(tsvWriter: TsvWriter): Unit = {
    scrapeModels(tsvWriter)
    scrapeIndicators(tsvWriter)
  }
}

object DojoFileScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def fromConfig(config: Config): DojoFileScraper = {
    val filename = config.getString("DojoFileScraper.filename")

    new DojoFileScraper(filename)
  }
}
