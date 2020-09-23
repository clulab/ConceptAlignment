package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibGloveIndexerApp extends App {
  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexGlove()
}
