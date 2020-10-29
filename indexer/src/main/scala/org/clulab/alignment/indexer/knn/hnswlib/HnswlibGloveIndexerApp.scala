package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibGloveIndexerApp extends App {
  val indexFilename = args(0)

  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexGlove(indexFilename)
}
