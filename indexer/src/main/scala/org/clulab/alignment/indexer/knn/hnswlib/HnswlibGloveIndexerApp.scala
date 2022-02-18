package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibGloveIndexerApp extends App {

  def run(indexFilename: String, countOpt: Option[Int] = None): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexGlove(indexFilename, countOpt)
  }

  run(args(0))
}
