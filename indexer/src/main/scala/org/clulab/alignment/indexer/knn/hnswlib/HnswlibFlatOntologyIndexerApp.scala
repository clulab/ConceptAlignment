package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibFlatOntologyIndexerApp extends App {
  val indexFilename = args(0)

  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexFlatOntology(indexFilename)
}
