package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.SearchResult
import com.github.jelmerk.knn.scalalike.floatCosineDistance
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.GloveAlignmentItem

object GloveIndex {
  val dimensions = 300

  type Index = HnswIndex[String, Array[Float], GloveAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.loadFromFile[String, Array[Float], GloveAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[GloveAlignmentItem]): Index = {
    val index = HnswIndex[String, Array[Float], GloveAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index
  }

  def find(index: Index, id: String): Option[Array[Float]] = {
    val gloveAlignmentItemOpt = index.get(id)
    val vectorOpt = gloveAlignmentItemOpt.map(_.vector)

    vectorOpt
  }
}
