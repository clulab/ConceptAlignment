package org.clulab.alignment.indexer.knn.hnswlib

import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex

class HnswlibIndexerApp(hnswlibLocations: HnswlibLocationsTrait) {
  val hnswlibIndexer = new HnswlibIndexer()

  def run(glove: Boolean = false): DatamartIndex.Index = {
    // Control these through boolean arguments.
    // hnswlibIndexer.indexSample()
    // hnswlibIndexer.indexOntology()
    val datamartIndex = hnswlibIndexer.indexDatamart(hnswlibLocations.datamartFilename, hnswlibLocations.datamartIndexFilename)
    if (glove)
      hnswlibIndexer.indexGlove(hnswlibLocations.gloveIndexFilename)

    datamartIndex
  }
}

class StaticHnswlibLocations(val datamartFilename: String, val datamartIndexFilename: String, val gloveIndexFilename: String) extends HnswlibLocationsTrait {
}

object HnswlibIndexerApp extends App {
  val datamartFilename = args(0)
  val datamartIndexFilename = args(1)
  val gloveIndexFilename = args(2)
  val glove = args(3).toBoolean

  new HnswlibIndexerApp(new StaticHnswlibLocations(datamartFilename, datamartIndexFilename, gloveIndexFilename)).run(glove)
}
