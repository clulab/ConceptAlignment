package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibCompositionalOntologyIndexerApp extends App {
  val conceptIndexFilename = args(0)
  val processIndexFilename = args(1)
  val propertyIndexFilename = args(2)

  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexCompositionalOntology(conceptIndexFilename, processIndexFilename, propertyIndexFilename)
}
