package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.wm.eidos.utils.TsvWriter

abstract class DataMartScraper {
  def scrape(tsvWriter: TsvWriter): Unit
}

object DataMartScraper {

  def apply(config: Config, name: String): DataMartScraper = {
    name match {
      case "IsiScraper" => IsiScraper.fromConfig(config)
      case "NyuScraper" => NyuScraper.fromConfig(config)
      case "SuperMaasScraper" => SuperMaasScraper.fromConfig(config)
      case _ => throw new Exception(s"Scraper name $name is not recognized.")
    }
  }
}