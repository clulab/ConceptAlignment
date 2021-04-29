package org.clulab.alignment.indexer.knn.hnswlib

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.Test
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex

class TestIndexer extends Test {
  val indexer = new HnswlibIndexer()

  it should "divulge all results" in {
    // Make an index of an ontology.  Go through every one, see if get all results back.
    // Make sure range is from +1 to -1
    val items = indexer.readFlatOntologyItems()
    val index = FlatOntologyIndex.newIndex(items)

    items.foreach { item =>
      val vector = item.vector
      val neighbors = index.findNearest(vector, k = items.size).toArray

      neighbors.size should be (items.size)
      neighbors.head.distance should be (0f)
    }
  }
}
