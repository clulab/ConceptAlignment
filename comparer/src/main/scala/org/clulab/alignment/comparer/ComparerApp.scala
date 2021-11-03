package org.clulab.alignment.comparer

import org.clulab.alignment.utils.{Sourcer, TsvReader}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.wm.eidoscommon.utils.FileUtils

object ComparerApp extends App {
  // Should this be from the index instead?
  val tsvFilename = "../comparer/datamarts.tsv"
  val jsonFilename = "../comparer/data-datacube-dump.json"

  case class TsvRecord(
    datamart_id: String,
    dataset_id: String,
    dataset_name: String,
    dataset_tags: String,
    dataset_description: String,
    dataset_url: String,
    variable_id: String,
    variable_name: String,
    variable_tags: String,
    variable_description: String
  ) {
    def this(strings: Array[String]) = this(
      strings(0), strings(1), strings(2), strings(3), strings(4),
      strings(5), strings(6), strings(7), strings(8), strings(9)
    )
  }

  def readTsv(): Array[TsvRecord] = {
    val tsvReader = new TsvReader()

    Sourcer.sourceFromFile(tsvFilename).autoClose { source =>
      source
          .getLines
          .map { line => new TsvRecord(tsvReader.readln(line)) }
          .toArray
    }
  }

  case class JsonRecord(name: String, outputName: String)

  def readJson(): Array[JsonRecord] = {
    val jsonString = FileUtils.getTextFromFile(jsonFilename)
    val jValue = ujson.read(jsonString)
    val jArray = jValue.arr
    val jsonRecords = jArray.flatMap { jValue =>
      val jObject = jValue.obj
      val name = jObject.value("name").str
      val outputs = jObject.value("outputs").arr
      val jsonRecords = outputs.map { output =>
        val outputName = output.obj("name").str
        JsonRecord(name, outputName)
      }.toArray

      jsonRecords
    }.toArray

    jsonRecords
  }

  val tsvRecords = readTsv()
  val jsonRecords = readJson()

  val tsvMap = tsvRecords.map { tsvRecord =>
    tsvRecord.variable_id -> tsvRecord
  }.toMap
  val jsonMap = jsonRecords.map { jsonRecord =>
    jsonRecord.outputName -> jsonRecord
  }.toMap

  val tsvKeys = tsvMap.keySet
  val jsonKeys = jsonMap.keySet

  val onlyInTsv = tsvKeys -- jsonKeys
  println("Only in tsv:")
  println(onlyInTsv.mkString("\n"))

  println()

  val onlyInJson = jsonKeys -- tsvKeys
  println("Only in json:")
  println(onlyInJson.mkString("\n")) // Add name from jsonRecord
}
