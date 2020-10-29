package org.clulab.alignment.scraper

import com.typesafe.config.ConfigFactory
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.TsvWriter

import scala.collection.JavaConverters._

class ScraperApp(scraperLocations: ScraperLocationsTrait = ScraperLocations.defaultLocations) {

  def run(scrapers: Seq[DatamartScraper]): Unit = {
    new TsvWriter(FileUtils.printWriterFromFile(scraperLocations.datamartFilename), isExcel = false).autoClose { tsvWriter =>
      tsvWriter.println("datamart_id",
        "dataset_id", "dataset_name", "dataset_description", "dataset_url",
        "variable_id", "variable_name", "variable_description"
      )
      scrapers.foreach(_.scrape(tsvWriter))
    }
  }
}

class StaticScraperLocations(filename: String) extends ScraperLocationsTrait {
  val datamartFilename: String = filename
}

object ScraperApp extends App {

  def getScrapers: Seq[DatamartScraper] = {
    val config = ConfigFactory.load
    val scraperNames = config.getStringList("Scraper.scrapers").asScala
    val scrapers = scraperNames.map { scraperName => DatamartScraper(config, scraperName) }

    scrapers
  }

  val datamartFilename = args(0)

  new ScraperApp(new StaticScraperLocations(datamartFilename)).run(getScrapers)
}
