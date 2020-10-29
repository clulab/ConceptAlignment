package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibGloveIndexerApp extends App {
  val hnswlibIndexer = new HnswlibIndexer()
  val indexFilename = args(0)

  hnswlibIndexer.indexGlove(indexFilename)
}
