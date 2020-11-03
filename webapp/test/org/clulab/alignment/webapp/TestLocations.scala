package org.clulab.alignment.webapp

import org.clulab.alignment.Test
import org.clulab.alignment.webapp.indexer.IndexerLocations
import org.clulab.alignment.webapp.utils.AutoLocations

class TestLocations extends Test {

  behavior of "locations"

  it should "work" in {
    val autoLocations = new AutoLocations()
    val indexerLocations = new IndexerLocations(autoLocations.index, autoLocations.baseDir, autoLocations.baseFile)

    indexerLocations.index should be (autoLocations.index)
    indexerLocations.baseDir should be (autoLocations.baseDir)
    indexerLocations.baseFile should be (autoLocations.baseFile)
  }
}
