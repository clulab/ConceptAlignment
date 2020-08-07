package org.clulab.alignment.indexer.knn.hnswlib.item

import com.github.jelmerk.knn.scalalike.Item
import org.clulab.alignment.data.datamart.DatamartIdentifier

case class DatamartAlignmentItem(id: DatamartIdentifier, vector: Array[Float]) extends Item[DatamartIdentifier, Array[Float]] {
  override def dimensions(): Int = vector.length
}
