package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibDatamartIndexerApp extends App {

  def run(datamartFileName: String, indexFilename: String): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexDatamart(datamartFileName, indexFilename)
  }

  run(args(0), args(1))
}
