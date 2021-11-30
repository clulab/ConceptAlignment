package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.utils.TsvWriter

abstract class DatamartScraper {
  def scrape(tsvWriter: TsvWriter): Unit
}

object DatamartScraper {

  def apply(config: Config, name: String): DatamartScraper = {
    name match {
      case "IsiScraper" => IsiScraper.fromConfig(config)
      case "NyuScraper" => NyuScraper.fromConfig(config)
      case "SuperMaasScraper" => SuperMaasScraper.fromConfig(config)
      case "DojoFileScraper" => DojoFileScraper.fromConfig(config)
      case "DojoRestScraper" => DojoRestScraper.fromConfig(config)
      case _ => throw new Exception(s"Scraper name $name is not recognized.")
    }
  }
}