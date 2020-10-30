package org.clulab.alignment.webapp

import org.clulab.alignment.Test
import org.clulab.alignment.webapp.indexer.Indexer
import org.clulab.alignment.webapp.indexer.IndexerLocations
import org.clulab.alignment.webapp.searcher.AutoSearchLocations

class TestIndexer extends Test {

  behavior of "Indexer"

  it should "work" in {
    val index = AutoSearchLocations.getNextIndex
    val indexerLocations = new IndexerLocations(index)
    val indexer = new Indexer(indexerLocations)

    indexer.run()
    indexerLocations.delete()
  }
}
