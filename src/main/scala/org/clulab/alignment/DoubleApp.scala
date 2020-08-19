package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.SingleLuceneApp.datamartFilename
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.OntologyIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem
import org.clulab.alignment.searcher.lucene.LuceneSearcher
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

import scala.collection.mutable

object DoubleApp extends App {
  val luceneDirname = "../lucene-datamart"
  val datamartFilename = "../hnswlib-datamart.idx"

  val field = "variableDescription"
  val maxHits = 10
  val queryString = "food"

  val luceneSearcher = new LuceneSearcher(luceneDirname, field)
  val knnIndex = DatamartIndex.load(datamartFilename)
  val vector = {
    val datamartDocument = luceneSearcher.datamartSearch(queryString, 1).next._2
    val id = DatamartIdentifier(datamartDocument.datamartId, datamartDocument.datasetId, datamartDocument.variableId)
    val datamartAlignmentItem = knnIndex(id)

    datamartAlignmentItem.vector
  }

  val size = knnIndex.size
  val luceneIterator = luceneSearcher.datamartSearch(queryString, size)
  val knnIterator = DatamartIndex.findNearest(knnIndex, vector)
/*
  def search(knnIterator: Iterator[SearchResult[DatamartAlignmentItem, Float]], luceneIterator: Iterator[(Float, DatamartDocument)], maxHits: Int): Array[SearchResult[DatamartAlignmentItem, Float]] = {
    val unmatchedKnnItems: mutable.Map[String, Float] = mutable.Map.empty
    val unmatchedLuceneItems: mutable.Map[String, Float] = mutable.Map.empty
    // id, knnScore, luceneScore, combinedScore
    val matchedItems: Array[(String, Float, Float, Float)] = new Array(maxHits)
    var count = 0

    def insert(datamartId: String, knnScore: Float, luceneScore: Float, combinedScore: Float): Unit = ()

    while (knnIterator.hasNext && luceneIterator.hasNext && count < maxHits) {
      val SearchResult(datamartAlignmentItem, knnScore) = knnIterator.next()
      val knnDatamartId = datamartAlignmentItem.id.toString
      val (luceneScore, datamartDocument) = luceneIterator.next()
      val luceneDatamartId = datamartDocument.datamartId
      var knnFinished = false
      var luceneFinished = false

      if (knnDatamartId == luceneDatamartId) {
        val combinedScore = knnScore * luceneScore

        insert(knnDatamartId, knnScore, luceneScore, combinedScore)
        count += 1
      }
      else if (unmatchedKnnItems.contains(luceneDatamartId)) {

      }
      else if (unmatchedLuceneItems) {

      }
      else {

      }
    }


    // If both ran out, then done
    // If neither ran out, figure out how far down one can go down the other one
    // and keep going until on or other runs out
    // If one ran out and count isn't full
    // If other ran out and count isn't full

    knnIterator.toArray
  }

  val datamartSearchResults = search(knnIterator, luceneIterator, maxHits)

  datamartSearchResults.map { case SearchResult(datamartAlignmentItem, value) =>
    val id = datamartAlignmentItem.id

    println(s"$id $value")
  }
 */
}
