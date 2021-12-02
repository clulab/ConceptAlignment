package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.utils.{Sourcer, TsvWriter}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.{HashSet => MutableHashSet}

class DojoFileScraper(modelsFilename: String, indicatorsFilename: String) extends DojoScraper {

  def scrapeModels(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    Sourcer.sourceFromFile(modelsFilename).autoClose { source =>
      source.getLines.foreach { line =>
        val jObj = ujson.read(line).obj
        val dojoDocument = new ModelDocument(jObj)
        val datasetId = dojoDocument.id

        DojoFileScraper.logger.info(s"Scraping DOJO model with datasetId $datasetId")
        dojoDocument.parameters.map { parameter =>
          writeDojoRecord(dojoDocument, parameter, tsvWriter, doubleIds, DojoFileScraper.logger)
        }
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

  def scrapeIndicators(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    Sourcer.sourceFromFile(indicatorsFilename).autoClose { source =>
      source.getLines.foreach { line =>
        val jObj = ujson.read(line).obj
        val dojoDocument = new IndicatorDocument(jObj)
        val datasetId = dojoDocument.id

        DojoFileScraper.logger.info(s"Scraping DOJO indicator with datasetId $datasetId")
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
    val modelsFilename = config.getString("DojoFileScraper.modelsFilename")
    val indicatorsFilename = config.getString("DojoFileScraper.indicatorsFilename")

    new DojoFileScraper(modelsFilename, indicatorsFilename)
  }
}
