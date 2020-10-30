package org.clulab.alignment.webapp

import org.clulab.alignment.Test
import org.clulab.alignment.searcher.knn.KnnLocations

class TestLocations extends Test {

  behavior of "locations"

  it should "work" in {
    val searcherLocations = try {
      new AutoKnnLocations()
    }
    catch {
      case _: Throwable => new KnnLocations()
    }
    val indexerLocations = new IndexerLocations(searcherLocations.index)

    searcherLocations.index should be  (indexerLocations.index)
    searcherLocations.baseDir should be  (indexerLocations.baseDir)
    searcherLocations.baseFile should be  (indexerLocations.baseFile)
  }
}
