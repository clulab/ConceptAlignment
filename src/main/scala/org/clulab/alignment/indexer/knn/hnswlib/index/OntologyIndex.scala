package org.clulab.alignment.indexer.knn.hnswlib.index

import java.io.File

import com.github.jelmerk.knn.scalalike.floatCosineDistance
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.OntologyAlignmentItem
import org.clulab.alignment.data.ontology.OntologyIdentifier

object OntologyIndex {
  val dimensions = 300

  type Index = HnswIndex[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float]

  def load(filename: String): Index = {
    val index = HnswIndex.load[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float](new File(filename))

    index.asInstanceOf[Index]
  }

  def newIndex(items: Iterable[OntologyAlignmentItem]): Index = {
    val index = HnswIndex[OntologyIdentifier, Array[Float], OntologyAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index
  }
}
