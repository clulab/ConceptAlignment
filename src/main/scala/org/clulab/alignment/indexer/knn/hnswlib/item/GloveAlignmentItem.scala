package org.clulab.alignment.indexer.knn.hnswlib.item

import com.github.jelmerk.knn.scalalike.Item

case class GloveAlignmentItem(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
  override def dimensions(): Int = vector.length
}
