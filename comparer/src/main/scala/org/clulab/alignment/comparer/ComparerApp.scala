package org.clulab.alignment.comparer

object ComparerApp extends App {
  // Should this be from the index instead?
  val tsvFilename = "../comparer/datamarts.tsv"
  val jsonFilename = "../comparer/data-datacube-dump.json"
  val csvAtaFilename = "../comparer/ATA2.csv"
  val csvNafFilename = "../comparer/NAF2.csv"

  val jsonRecords = Shared.readJsonRecords(jsonFilename)
  val jsonOutputNameMap = jsonRecords.map { jsonRecord =>
    jsonRecord.outputName -> jsonRecord
  }.toMap
  val jsonOutputNames = jsonOutputNameMap.keySet
  val jsonOutputDisplayNameMap = jsonRecords.map { jsonRecord =>
    jsonRecord.outputDisplayName -> jsonRecord
  }.toMap
  val jsonOutputDisplayNames = jsonOutputDisplayNameMap.keySet

  def compareJsonToTsv() = {
    val tsvRecords = Shared.readTsvRecords(tsvFilename)
    val tsvMap = tsvRecords.map { tsvRecord =>
      tsvRecord.variable_id -> tsvRecord
    }.toMap
    val tsvKeys = tsvMap.keySet

    val onlyInTsv = tsvKeys -- jsonOutputNames // jsonOutputNames
    println(s"Only in tsv: ${onlyInTsv.size}")
    println(onlyInTsv.mkString("\n"))

    println()

    val onlyInJson = jsonOutputNames -- tsvKeys
    println(s"Only in json: ${onlyInJson.size}")
    println(onlyInJson.mkString("\n")) // Add name from jsonRecord

    val intersection = tsvKeys.intersect(jsonOutputNames) // jsonOutputNames
    println(s"Intersection: ${intersection.size}")
  }

  def compareJsonToCsv(): Unit = {
    val csvAtaRecords = Shared.readCsvRecords(csvAtaFilename)
    val csvNafRecords = Shared.readCsvRecords(csvNafFilename)

    def compareRecords(csvRecords: Array[Shared.CsvRecord]): Unit = {
      csvRecords.foreach { csvRecord =>
        if (csvRecord.assigned != "null" && !jsonOutputDisplayNames.contains(csvRecord.assigned))
          println(csvRecord.assigned)
        if (csvRecord.default != "null" && !jsonOutputDisplayNames.contains(csvRecord.default))
          println(csvRecord.default)
      }
    }

    println(s"Only in Ata:")
    compareRecords(csvAtaRecords)

    println()

    println(s"Only in Naf:")
    compareRecords(csvNafRecords)
  }

//  compareJsonToCsv()
  compareJsonToTsv()
}
