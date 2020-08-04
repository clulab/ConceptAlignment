package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.utils.TsvWriter

class NyuScraper extends DatamartScraper {
  def scrape(tsvWriter: TsvWriter): Unit = {}
}

object NyuScraper {

  def fromConfig(config: Config): NyuScraper = {
    new NyuScraper
  }
}