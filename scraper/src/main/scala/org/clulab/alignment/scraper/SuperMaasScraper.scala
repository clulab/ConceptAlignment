package org.clulab.alignment.scraper

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SuperMaasScraper(baseUrl: String, createdSince: String = "") extends DatamartScraper {
  val modelScraper = new SuperMaasModelScraper(baseUrl, createdSince)
  val cubeScraper = new SuperMaasCubeScraper(baseUrl, createdSince)

  def scrape(tsvWriter: TsvWriter): Unit = {
    modelScraper.scrape(tsvWriter)
    cubeScraper.scrape(tsvWriter)
  }
}

object SuperMaasScraper {
  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val stanza = "SuperMaasScraper"
  val urlKey = s"$stanza.url"

  def fromConfig(config: Config): SuperMaasScraper = {
    val baseUrl = config.getString(urlKey)
    val createdSince = config.getString(s"$stanza.createdSince")

    new SuperMaasScraper(baseUrl, createdSince)
  }

  def setUrl(config: Config, url: String): Config = {
    config.withValue(urlKey, ConfigValueFactory.fromAnyRef(url))
  }
}
