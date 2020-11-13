package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibOntologyIndexerApp extends App {
  val indexFilename = args(0)

  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexOntology(indexFilename)
}
