package org.clulab.alignment.comparer

import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, Sourcer, TsvReader, TsvWriter}

import java.io.File

// At the time of the experiment, we didn't include some of these new columns.
// They are added here so that ExperimentSpreadsheetsApp can run in this branch.
object AddColumnsApp extends App {
  val inputFilename = "./comparer/src/main/resources/org/clulab/alignment/comparer/datamarts/datamarts.tsv"
  val outputFilename = "./AddColumnsApp.tsv"
  val location = 6

  Sourcer.sourceFromFile(new File(inputFilename)).autoClose { source =>
    val tsvReader = new TsvReader()

    FileUtils.printWriterFromFile(outputFilename).autoClose { printWriter =>
      val tsvWriter = new TsvWriter(printWriter, isExcel = false)

      tsvWriter.println(
        "datamart_id", "dataset_id", "dataset_name", "dataset_tags", "dataset_description",
        "dataset_url", "dataset_geography", "dataset_period_gte", "dataset_period_lte", "variable_id",
        "variable_name", "variable_tags", "variable_description"
      )
      source.getLines.foreach { line =>
        val values = tsvReader.readln(line)
        val newValues = values.slice(0, location) ++
          // dataset_geography, dataset_period_gte, dataset_period_lte
          Array("[]", "", "") ++
          values.slice(location, values.length)

        tsvWriter.println(newValues)
      }
    }
  }
}
