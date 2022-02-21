package org.clulab.alignment.comparer

import org.clulab.alignment.scraper.DojoFileScraper
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, TsvWriter}

object ScrapeIndicators extends App {
  val dojoFileScraper = new DojoFileScraper("", "../comparer/indicators_11082021.jsonl")

  // Note: the qualifierOutputs need to be turned on to match previous behavior.
  FileUtils.printWriterFromFile("../comparer/datamarts-new.tsv").autoClose { printWriter =>
    val tsvWriter = new TsvWriter(printWriter, isExcel = false)
    tsvWriter.println(
      "datamart_id", "dataset_id", "dataset_name", "dataset_tags", "dataset_description",
      "dataset_period_lte", "variable_id", "variable_name", "variable_tags", "variable_description"
    )
    dojoFileScraper.scrapeIndicators(tsvWriter)
  }
}
