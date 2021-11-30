package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibCompositionalOntologyIndexerApp extends App {

  def run(conceptIndexFilename: String, processIndexFilename: String, propertyIndexFilename: String): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexCompositionalOntology(conceptIndexFilename, processIndexFilename, propertyIndexFilename)
  }

  run(args(0), args(1), args(2))
}
