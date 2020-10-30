package org.clulab.alignment.webapp.indexer

import org.clulab.alignment.Test
import org.clulab.alignment.webapp.searcher.AutoSearchLocations
import org.clulab.alignment.webapp.utils.AutoLocations

class TestIndexer extends Test {

  behavior of "Indexer"

  it should "work" in {
    val index = AutoLocations.getBaseIndexOpt.get
    val indexerLocations = new IndexerLocations(index)
    val indexer = new Indexer(indexerLocations)

    indexer.run()
    indexerLocations.delete()
  }
}
