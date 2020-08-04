package org.clulab.alignment.searcher.knn.hnswlib

import com.github.jelmerk.knn.scalalike.Item

case class TestAlignmentItem(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
  override def dimensions(): Int = vector.length
}

case class GloveAlignmentItem(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
  override def dimensions(): Int = vector.length
}

case class OntologyAlignmentItem(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
  override def dimensions(): Int = vector.length
}

case class DatamartAlignmentItem(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
  override def dimensions(): Int = vector.length
}
