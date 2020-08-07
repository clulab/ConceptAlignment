package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.floatCosineDistance
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem

object DatamartIndex {
  val dimensions = 300

  type Index = HnswIndex[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.load[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[DatamartAlignmentItem]): Index = {
    val index = HnswIndex[DatamartIdentifier, Array[Float], DatamartAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index
  }
}
