package org.clulab.alignment.indexer.knn.hnswlib.item

import com.github.jelmerk.knn.scalalike.Item
import org.clulab.alignment.data.ontology.OntologyIdentifier

case class OntologyAlignmentItem(id: OntologyIdentifier, vector: Array[Float]) extends Item[OntologyIdentifier, Array[Float]] {
  override def dimensions(): Int = vector.length
}
