package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibGloveIndexerApp extends App {

  def run(indexFilename: String): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexGlove(indexFilename)
  }

  run(args(0))
}
