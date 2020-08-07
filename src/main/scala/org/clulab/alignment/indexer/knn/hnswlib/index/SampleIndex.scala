package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.floatCosineDistance
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.SampleAlignmentItem

object SampleIndex {
  val dimensions = 4

  type Index = HnswIndex[String, Array[Float], SampleAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.load[String, Array[Float], SampleAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[SampleAlignmentItem]): Index = {
    val index = HnswIndex[String, Array[Float], SampleAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index
  }
}
