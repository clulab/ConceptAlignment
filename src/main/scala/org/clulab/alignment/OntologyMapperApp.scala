package org.clulab.alignment

import java.io.PrintWriter

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.OntologyIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.{DatamartIndex, OntologyIndex}
import org.clulab.alignment.indexer.knn.hnswlib.item.{DatamartAlignmentItem, OntologyAlignmentItem}
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.SafeScore
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.Json

case class OntologyToDatamarts(srcId: OntologyIdentifier, dstResults: Seq[SearchResult[DatamartAlignmentItem, Float]]) {

  def toJsObject: JsObject = {
    val ontologyIdentifier = srcId
    val searchResults = dstResults.toArray
    val jsDatamartValues = searchResults.map { searchResult =>
      Json.obj(
        "score" -> SafeScore.get(searchResult.distance),
        "datamart" -> searchResult.item.id.toJsObject
      )
    }
    Json.obj(
      "ontology" -> ontologyIdentifier.nodeName,
      "datamarts" -> JsArray(jsDatamartValues)
    )
  }
}

case class DatamartToOntologies(srcId: DatamartIdentifier, dstResults: Seq[SearchResult[OntologyAlignmentItem, Float]]) {

  def toJsObject: JsObject = {
    val datamartIdentifier = srcId
    val searchResults = dstResults.toArray
    val jsOntologyValues = searchResults.map { searchResult =>
      Json.obj(
        "score" -> SafeScore.get(searchResult.distance),
        "ontology" -> searchResult.item.id.nodeName
      )
    }
    Json.obj(
      "datamart" -> datamartIdentifier.toJsObject,
      "ontologies" -> JsArray(jsOntologyValues)
    )
  }
}

class OntologyMapper(val datamartIndex: DatamartIndex.Index, val ontologyIndex: OntologyIndex.Index) {

  def ontologyToDatamartMapping(topKOpt: Option[Int] = Some(datamartIndex.size), thresholdOpt: Option[Float] = None): Iterator[OntologyToDatamarts] = {
    val ontologyItems = ontologyIndex.iterator
    val ontologyToDatamarts = ontologyItems.map { ontologyItem =>
      val searchResults = alignOntologyItemToDatamart(ontologyItem, topKOpt.getOrElse(datamartIndex.size), thresholdOpt)
      OntologyToDatamarts(ontologyItem.id, searchResults)
    }
    ontologyToDatamarts
  }

  def datamartToOntologyMapping(topKOpt: Option[Int] = Some(ontologyIndex.size), thresholdOpt: Option[Float] = None): Iterator[DatamartToOntologies] = {
    val datamartItems = datamartIndex.iterator
    val datamartToOntologies = datamartItems.map { datamartItem =>
      val searchResults = alignDatamartItemToOntology(datamartItem, topKOpt.getOrElse(ontologyIndex.size), thresholdOpt)
      DatamartToOntologies(datamartItem.id, searchResults)
    }
    datamartToOntologies
  }

  def alignOntologyItemToDatamart(ontologyItem: OntologyAlignmentItem, topK: Int = datamartIndex.size, thresholdOpt: Option[Float]): Seq[SearchResult[DatamartAlignmentItem, Float]] = {
    val ontologyNodeVector = ontologyItem.vector
    val result = DatamartIndex.findNearest(datamartIndex, ontologyNodeVector, topK, thresholdOpt)
    result
  }

  def alignDatamartItemToOntology(datamartItem: DatamartAlignmentItem, topK: Int = ontologyIndex.size, thresholdOpt: Option[Float]): Seq[SearchResult[OntologyAlignmentItem, Float]] = {
    val datamartNodeVector = datamartItem.vector
    val result = OntologyIndex.findNearest(ontologyIndex, datamartNodeVector, topK, thresholdOpt)
    result
  }
}

object OntologyMapperApp extends App {
  val datamartIndexFilename = args.lift(0).getOrElse("../hnswlib-datamart.idx")
  val ontologyIndexFilename = args.lift(1).getOrElse("../hnswlib-wm_flattened.idx")
  val datamartMappingFilename = args.lift(2).getOrElse("../datamartMapping.json")
  val ontologyMappingFilename = args.lift(3).getOrElse("../ontologyMapping.json")
  val limitOpt = args.lift(4).map(_.toInt)

  val startTime = System.currentTimeMillis
  val datamartIndex = DatamartIndex.load(datamartIndexFilename)
  val ontologyIndex = OntologyIndex.load(ontologyIndexFilename)
  val mapper = new OntologyMapper(datamartIndex, ontologyIndex)

  {
    val allDatamartToOntologies = mapper.datamartToOntologyMapping(limitOpt)
    val allJsValues = allDatamartToOntologies.map(_.toJsObject).toArray
    val jsValue = JsArray(allJsValues)
    val printWriter = FileUtils.printWriterFromFile(ontologyMappingFilename)

    printWriter.println(jsValue.toString)
    printWriter.close()
  }

  {
    val allOntologyToDatamarts = mapper.ontologyToDatamartMapping(limitOpt)
    val allJsValues = allOntologyToDatamarts.map(_.toJsObject).toArray
    val jsValue = JsArray(allJsValues)
    val printWriter = FileUtils.printWriterFromFile(datamartMappingFilename)

    printWriter.println(jsValue.toString)
    printWriter.close()
  }
  val endTime = System.currentTimeMillis
  val diffTime = endTime - startTime
  println(s"$endTime - $startTime = $diffTime")
}
