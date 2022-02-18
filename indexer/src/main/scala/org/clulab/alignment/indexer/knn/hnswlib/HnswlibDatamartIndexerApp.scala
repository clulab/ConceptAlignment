package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibDatamartIndexerApp extends App {

  def run(datamartFileName: String, indexFilename: String): Unit = {
    new HnswlibIndexer().indexDatamart(datamartFileName, indexFilename)
  }

   run(args(0), args(1))
}
