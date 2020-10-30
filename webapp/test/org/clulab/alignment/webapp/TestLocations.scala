package org.clulab.alignment.webapp

import org.clulab.alignment.Test
import org.clulab.alignment.searcher.knn.KnnLocations
import org.clulab.alignment.webapp.indexer.IndexerLocations
import org.clulab.alignment.webapp.searcher.AutoSearchLocations

class TestLocations extends Test {

  behavior of "locations"

  it should "work" in {
    val searcherLocations = try {
      new AutoSearchLocations()
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
