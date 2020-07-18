package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.wm.eidos.utils.TsvWriter

class SuperMaasScraper extends DataMartScraper {
  def scrape(tsvWriter: TsvWriter): Unit = {}
}

object SuperMaasScraper {

  def fromConfig(config: Config): SuperMaasScraper = {
    new SuperMaasScraper
  }
}