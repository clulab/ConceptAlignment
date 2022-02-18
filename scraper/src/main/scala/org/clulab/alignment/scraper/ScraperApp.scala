package org.clulab.alignment.scraper

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.TsvWriter

import scala.collection.JavaConverters._

class ScraperApp(scraperLocations: ScraperLocationsTrait = ScraperLocations.defaultLocations) {

  def run(scrapers: Seq[DatamartScraper]): Unit = {
    new TsvWriter(FileUtils.printWriterFromFile(scraperLocations.datamartFilename), isExcel = false).autoClose { tsvWriter =>
      tsvWriter.println(Scraper.headers)
      scrapers.foreach(_.scrape(tsvWriter))
    }
  }
}

class StaticScraperLocations(filename: String) extends ScraperLocationsTrait {
  val datamartFilename: String = filename
}

object Scraper {
  // Since ScraperApp is an App, this won't be initialized automatically.

  val headers = Seq(
    "datamart_id",
    "dataset_id",
    "dataset_name",
    "dataset_tags",
    "dataset_description",
    "dataset_url",

    "dataset_geography",
    "dataset_period_gte",
    "dataset_period_lte",

    "variable_id",
    "variable_name",
    "variable_tags",
    "variable_description"
  )
}

object ScraperApp extends App {

  def getScrapers(supermaasUrl: String): Seq[DatamartScraper] = {
    val config = SuperMaasScraper.setUrl(ConfigFactory.load, supermaasUrl)

    getScrapers(config)
  }

  def getScrapers: Seq[DatamartScraper] = getScrapers(ConfigFactory.load)

  def getScrapers(config: Config): Seq[DatamartScraper] = {
    val scraperNames = config.getStringList("Scraper.scrapers").asScala
    val scrapers = scraperNames.map { scraperName => DatamartScraper(config, scraperName) }

    scrapers
  }

  def run(datamartFilename: String): Unit = {
    new ScraperApp(new StaticScraperLocations(datamartFilename)).run(getScrapers)
  }

  run(args(0))
}
