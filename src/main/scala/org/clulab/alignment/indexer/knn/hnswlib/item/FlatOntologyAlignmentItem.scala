package org.clulab.alignment.indexer.knn.hnswlib.item

import com.github.jelmerk.knn.scalalike.Item
import org.clulab.alignment.data.ontology.FlatOntologyIdentifier

case class FlatOntologyAlignmentItem(id: FlatOntologyIdentifier, vector: Array[Float]) extends Item[FlatOntologyIdentifier, Array[Float]] {
  override def dimensions(): Int = vector.length
}
