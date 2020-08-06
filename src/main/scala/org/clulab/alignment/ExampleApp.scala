package org.clulab.alignment

import java.io.File

import com.github.jelmerk.knn.scalalike.SearchResult
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.searcher.lucene.{Searcher => LuceneSearcher}
import org.clulab.alignment.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.knn.hnswlib.DatamartAlignmentItem

object ExampleApp extends App {
  val luceneDir = "../lucene"
  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-ontology.idx"

  val field = "variableDescription"
  val maxHits = 10
  val queryString = "food"

  val luceneSearcher = new LuceneSearcher(luceneDir, field)
  val datamartIndex = HnswIndex.load[String, Array[Float], DatamartAlignmentItem, Float](new File(datamartFilename))
  val ontologyIndex = HnswIndex.load[String, Array[Float], DatamartAlignmentItem, Float](new File(ontologyFilename))

  // In this example, just take the single best search result.
  val documentOpt = luceneSearcher.search(queryString, 1).headOption.map(_._2)
  val datamartItemOpt = documentOpt.flatMap { document =>
    val datamartId = document.get("datamartId")
    val datasetId = document.get("datasetId")
    val variableId = document.get("variableId")
    val id = new DatamartIdentifier(datamartId, datasetId, variableId).toString
    val datamartAlignmentItemOpt = datamartIndex.get(id)

    datamartAlignmentItemOpt
  }
  val resultsOpt = datamartItemOpt.map { datamartItem =>
    val id = datamartItem.id
    val vector = datamartItem.vector

    println(s"$id =>")
    ontologyIndex.findNearest(vector, maxHits).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }
}
