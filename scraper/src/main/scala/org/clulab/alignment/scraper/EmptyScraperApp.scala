package org.clulab.alignment.scraper

import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.TsvWriter

class EmptyScraperApp(scraperLocations: ScraperLocationsTrait = ScraperLocations.defaultLocations) {

  def run(): Unit = {
    new TsvWriter(FileUtils.printWriterFromFile(scraperLocations.datamartFilename), isExcel = false).autoClose { tsvWriter =>
      tsvWriter.println(ScraperApp.headers)
    }
  }
}

object EmptyScraperApp extends App {
  val datamartFilename = args(0)

  new EmptyScraperApp(new StaticScraperLocations(datamartFilename)).run()
}
