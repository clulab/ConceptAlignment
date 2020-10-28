package org.clulab.alignment.scraper

import com.typesafe.config.ConfigFactory
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.TsvWriter

import scala.collection.JavaConverters._

class SuperMaasScraperApp(filename: String) {
  val config = ConfigFactory.load
  val scraper = SuperMaasScraper.fromConfig(config)

  def run(): Unit = {
    new TsvWriter(FileUtils.printWriterFromFile(filename), isExcel = false).autoClose { tsvWriter =>
      tsvWriter.println("datamart_id",
        "dataset_id", "dataset_name", "dataset_description", "dataset_url",
        "variable_id", "variable_name", "variable_description"
      )
      scraper.scrape(tsvWriter)
    }
  }
}

object SuperMaasScraperApp extends App {
  new SuperMaasScraperApp(args(0)).run()
}
