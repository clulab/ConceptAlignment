package org.clulab.alignment

import java.io.PrintWriter
import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.FlatOntologyIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.{DatamartIndex, FlatOntologyIndex}
import org.clulab.alignment.indexer.knn.hnswlib.item.{DatamartAlignmentItem, FlatOntologyAlignmentItem}
import org.clulab.alignment.utils.FileUtils
import org.clulab.alignment.utils.SafeScore
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.Json

case class FlatOntologyToDatamarts(srcId: FlatOntologyIdentifier, dstResults: Seq[SearchResult[DatamartAlignmentItem, Float]]) {

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

case class DatamartToFlatOntologies(srcId: DatamartIdentifier, dstResults: Seq[SearchResult[FlatOntologyAlignmentItem, Float]]) {

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

class FlatOntologyMapper(val datamartIndex: DatamartIndex.Index, val ontologyIndex: FlatOntologyIndex.Index) {

  def ontologyToDatamartMapping(topKOpt: Option[Int] = Some(datamartIndex.size), thresholdOpt: Option[Float] = None): Iterator[FlatOntologyToDatamarts] = {
    val ontologyItems = ontologyIndex.iterator
    val ontologyToDatamarts = ontologyItems.map { ontologyItem =>
      val searchResults = alignOntologyItemToDatamart(ontologyItem, topKOpt.getOrElse(datamartIndex.size), thresholdOpt)
      FlatOntologyToDatamarts(ontologyItem.id, searchResults)
    }
    ontologyToDatamarts
  }

  def datamartToOntologyMapping(topKOpt: Option[Int] = Some(ontologyIndex.size), thresholdOpt: Option[Float] = None): Iterator[DatamartToFlatOntologies] = {
    val datamartItems = datamartIndex.iterator
    val datamartToOntologies = datamartItems.map { datamartItem =>
      val searchResults = alignDatamartItemToOntology(datamartItem, topKOpt.getOrElse(ontologyIndex.size), thresholdOpt)
      DatamartToFlatOntologies(datamartItem.id, searchResults)
    }
    datamartToOntologies
  }

  def alignOntologyItemToDatamart(ontologyItem: FlatOntologyAlignmentItem, topK: Int = datamartIndex.size, thresholdOpt: Option[Float]): Seq[SearchResult[DatamartAlignmentItem, Float]] = {
    val ontologyNodeVector = ontologyItem.vector
    val result = DatamartIndex.findNearest(datamartIndex, ontologyNodeVector, topK, thresholdOpt)
    result
  }

  def alignVectorToOntology(vector: Array[Float], topK: Int = datamartIndex.size, thresholdOpt: Option[Float]): Seq[SearchResult[FlatOntologyAlignmentItem, Float]] = {
    val result = FlatOntologyIndex.findNearest(ontologyIndex, vector, topK, thresholdOpt)
    result
  }

  def alignDatamartItemToOntology(datamartItem: DatamartAlignmentItem, topK: Int = ontologyIndex.size, thresholdOpt: Option[Float]): Seq[SearchResult[FlatOntologyAlignmentItem, Float]] = {
    alignVectorToOntology(datamartItem.vector, topK, thresholdOpt)
  }
}

object FlatOntologyMapper {

  def apply(flatOntologyMapperOpt: Option[FlatOntologyMapper], datamartIndex: DatamartIndex.Index, filename: String): FlatOntologyMapper = {
    val ontologyIndex = flatOntologyMapperOpt
        .map(_.ontologyIndex)
        .getOrElse(FlatOntologyIndex.load(filename))

    new FlatOntologyMapper(datamartIndex, ontologyIndex)
  }
}

object FlatOntologyMapperApp extends App {
  val datamartIndexFilename = args.lift(0).getOrElse("../hnswlib-datamart.idx")
  val ontologyIndexFilename = args.lift(1).getOrElse("../hnswlib-wm_flattened.idx")
  val datamartMappingFilename = args.lift(2).getOrElse("../flatDatamartMapping.json")
  val ontologyMappingFilename = args.lift(3).getOrElse("../flatOntologyMapping.json")
  val limitOpt = Some(args.lift(4).map(_.toInt).getOrElse(10))

  val startTime = System.currentTimeMillis
  val datamartIndex = DatamartIndex.load(datamartIndexFilename)
  val ontologyIndex = FlatOntologyIndex.load(ontologyIndexFilename)
  val mapper = new FlatOntologyMapper(datamartIndex, ontologyIndex)

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
