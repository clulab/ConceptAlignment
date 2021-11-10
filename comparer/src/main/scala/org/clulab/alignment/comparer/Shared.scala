package org.clulab.alignment.comparer

import org.clulab.alignment.CompositionalOntologyMapper
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{Sourcer, TsvReader}
import org.clulab.wm.eidoscommon.utils.{FileUtils, StringUtils}

object Shared {

  case class CsvRecord(node: String, assigned: String, default: String)

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

  case class JsonRecord(name: String, outputName: String, outputDisplayName: String)

  def readConceptMapLong(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] = {
    println("\nconceptMapLong:\n")
    val conceptIndex = compositionalOntologyMapper.conceptIndex
    val map = conceptIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val key =
          if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
          else nodeName

      println(s"$key -> $nodeName")
      key -> nodeName // Map nodeName without / to one with or without.
    }.toMap

    map
  }

  def readConceptMapShort(compositionalOntologyMapper: CompositionalOntologyMapper) = {
    println("\nconceptMapShort:\n")
    val conceptIndex = compositionalOntologyMapper.conceptIndex
    val map = conceptIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val withoutSlash =
          if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
          else nodeName
      val key = StringUtils.afterLast(withoutSlash, '/', true)

      println(s"$key -> $nodeName")
      key -> nodeName
    }.toMap

    map
  }

  def readPropertyMap(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] = {
    println("\npropertyMap:\n")
    val propertyIndex = compositionalOntologyMapper.propertyIndex
    val map = propertyIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val withoutSlash =
          if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
          else nodeName
      val key = StringUtils.afterLast(withoutSlash, '/', true)

      println(s"$key -> $nodeName")
      key -> nodeName
    }.toMap

    map
  }

  def readProcessMap(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] = {
    println("\nprocessMap:\n")
    val processIndex = compositionalOntologyMapper.processIndex
    val map = processIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val withoutSlash =
        if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
        else nodeName
      val key = StringUtils.afterLast(withoutSlash, '/', true)

      println(s"$key -> $nodeName")
      key -> nodeName
    }.toMap

    map
  }

  def readCsvRecords(filename: String): Array[CsvRecord] = {
    Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.map { line =>
        val firstComma = line.indexOf(',')
        val lastComma = line.lastIndexOf(',')

        val node = line.substring(0, firstComma)
        val assigned = line.substring(firstComma + 1, lastComma)
        val default = line.substring(lastComma + 1)

        CsvRecord(node, assigned, default)
      }.toArray
    }
  }

  def readTsvRecords(tsvFilename: String): Array[TsvRecord] = {
    val tsvReader = new TsvReader()

    Sourcer.sourceFromFile(tsvFilename).autoClose { source =>
      source
          .getLines
          .map { line => new TsvRecord(tsvReader.readln(line)) }
          .toArray
    }
  }

  def readJsonRecords(jsonFilename: String): Array[JsonRecord] = {
    val jsonString = FileUtils.getTextFromFile(jsonFilename)
    val jValue = ujson.read(jsonString)
    val jArray = jValue.arr
    val jsonRecords = jArray.flatMap { jValue =>
      val jObject = jValue.obj
      val name = jObject.value("name").str
      val outputs = jObject.value("outputs").arr
      val jsonRecords = outputs.map { output =>
        val outputName = output.obj("name").str
        val outputDisplayName = output.obj("display_name").str
        JsonRecord(name, outputName, outputDisplayName)
      }.toArray

      jsonRecords
    }.toArray

    jsonRecords
  }
}
