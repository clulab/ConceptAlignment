package org.clulab.alignment.indexer.knn.hnswlib

class HnswlibIndexerApp(hnswlibLocations: HnswlibLocationsTrait) {
  val hnswlibIndexer = new HnswlibIndexer()

  def run(glove: Boolean = false): Unit = {
    // Control these through boolean arguments.
    // hnswlibIndexer.indexSample()
    // hnswlibIndexer.indexOntology()
    if (glove)
      hnswlibIndexer.indexGlove(hnswlibLocations.gloveIndexFilename)
    hnswlibIndexer.indexDatamart(hnswlibLocations.datamartFilename, hnswlibLocations.datamartIndexFilename)
  }
}

class StaticHnswlibLocations(val datamartFilename: String, val datamartIndexFilename: String, val gloveIndexFilename: String) extends HnswlibLocationsTrait {
}

object HnswlibIndexerApp extends App {
  val datamartFilename = args(0)
  val datamartIndexFilename = args(1)
  val gloveIndexFilename = args(2)

  new HnswlibIndexerApp(new StaticHnswlibLocations(datamartFilename, datamartIndexFilename, gloveIndexFilename)).run(glove = true)
}
