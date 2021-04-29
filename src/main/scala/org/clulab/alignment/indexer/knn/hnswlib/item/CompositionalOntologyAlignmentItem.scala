package org.clulab.alignment.indexer.knn.hnswlib.item

import com.github.jelmerk.knn.scalalike.Item
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier

case class CompositionalOntologyAlignmentItem(id: CompositionalOntologyIdentifier, vector: Array[Float]) extends Item[CompositionalOntologyIdentifier, Array[Float]] {
  // The vector here, if ever it was to be used, might be the concatenation of the four flat vectors.
  override def dimensions(): Int = vector.length
}
