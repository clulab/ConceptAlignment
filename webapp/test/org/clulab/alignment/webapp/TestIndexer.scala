package org.clulab.alignment.webapp

import org.clulab.alignment.Test

class TestIndexer extends Test {

  behavior of "Indexer"

  it should "work" in {
    val index = AutoKnnLocations.getNextIndex
    val indexerLocations = new IndexerLocations(index)
    val indexer = new Indexer(indexerLocations)

    indexer.run()
    indexerLocations.delete()
  }
}
