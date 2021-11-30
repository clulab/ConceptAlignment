package org.clulab.alignment.comparer

import org.clulab.alignment.CompositionalOntologyMapper
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex
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

  // This reads all nodes, even branch nodes, and converts to a Map.
  class OntologyMapReader(flatOntologyIndex: FlatOntologyIndex.Index, message: String) {
    println(s"\n$message\n")

    def read(f: String => String): Map[String, String] = {
      flatOntologyIndex.map { flatOntologyAlignmentItem =>
        val nodeName = flatOntologyAlignmentItem.id.nodeName
        val key = f(nodeName)

        println(s"$key -> $nodeName")
        key -> nodeName
      }
    }.toMap
  }

  // This reads only leaf nodes and leaves them a Seq.
  class OntologyNodeReader(flatOntologyIndex: FlatOntologyIndex.Index, message: String) {
    println(s"\n$message\n")

    def read(): Seq[String] = {
      flatOntologyIndex.flatMap { flatOntologyAlignmentItem =>
        val nodeName = flatOntologyAlignmentItem.id.nodeName

        if (!nodeName.endsWith("/")) {
          println(s"$nodeName")
          Some(nodeName)
        }
        else None
      }
    }.toSeq
  }

  def nodeNameToLongKey(nodeName: String): String =
      if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
      else nodeName

  def nodeNameToShortKey(nodeName: String): String =
      StringUtils.afterLast(nodeNameToLongKey(nodeName), '/', true)

  def readConceptMapLong(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] =
      new OntologyMapReader(compositionalOntologyMapper.conceptIndex, "conceptMapLong")
          .read(nodeNameToLongKey)

  def readConceptMapShort(compositionalOntologyMapper: CompositionalOntologyMapper) =
      new OntologyMapReader(compositionalOntologyMapper.conceptIndex, "conceptMapShort")
          .read(nodeNameToShortKey)

  def readPropertyMap(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] =
      new OntologyMapReader(compositionalOntologyMapper.propertyIndex, "propertyMap")
          .read(nodeNameToShortKey)

  def readProcessMap(compositionalOntologyMapper: CompositionalOntologyMapper): Map[String, String] =
      new OntologyMapReader(compositionalOntologyMapper.processIndex, "processMap")
          .read(nodeNameToShortKey)

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
