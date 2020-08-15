package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.SearchResult
import com.github.jelmerk.knn.scalalike.floatInnerProduct
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem

import scala.collection.immutable

object DatamartIndex {
  val dimensions = 300

  type Index = HnswIndex[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.load[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[DatamartAlignmentItem]): Index = {
    val index = HnswIndex[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float](dimensions, floatInnerProduct, items.size)

    index.addAll(items)
    index
  }

  def findNearest(index: Index, vector: Array[Float]): Iterator[SearchResult[DatamartAlignmentItem, Float]] = {
    val maxHits = index.size

    findNearest(index, vector, maxHits)
  }

  def findNearest(index: Index, vector: Array[Float], maxHits: Int): Iterator[SearchResult[DatamartAlignmentItem, Float]] = {
    val nearest = index.findNearest(vector, k = maxHits)
    val largest = nearest.map { case SearchResult(item, value) =>
      SearchResult(item, 1f - value)
    }

    largest.iterator
  }
}
