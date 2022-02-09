package org.clulab.alignment.comparer

import org.clulab.alignment.scraper.DojoFileScraper
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, TsvWriter}

object ScrapeIndicators extends App {
  val dojoFileScraper = new DojoFileScraper("", "../comparer/indicators_11082021.jsonl")

  // Note: the qualifierOutputs need to be turned on to match previous behavior.
  FileUtils.printWriterFromFile("../comparer/datamarts-new.tsv").autoClose { printWriter =>
    val tsvWriter = new TsvWriter(printWriter, isExcel = false)
    dojoFileScraper.scrapeIndicators(tsvWriter)
  }
}
