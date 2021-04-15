package org.clulab.alignment.webapp.indexer

import org.clulab.alignment.Test
import org.clulab.alignment.webapp.utils.AutoLocations

class TestIndexer extends Test {

  behavior of "Indexer"

  it should "work" in {
    val index = AutoLocations.getBaseIndexOpt.get
    val indexerLocations = new IndexerLocations(index)
//    val indexer = new Indexer(indexerLocations, Seq.empty, null, None)

    // Add callback and delete when receive
//    indexer.run(None)
//    indexerLocations.delete()
  }
}
