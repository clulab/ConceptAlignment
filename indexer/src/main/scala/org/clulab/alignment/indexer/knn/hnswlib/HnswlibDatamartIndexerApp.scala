package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibDatamartIndexerApp extends App {
  val hnswlibIndexer = new HnswlibIndexer()
  val datamartFilename = args(0)
  val indexFilename = args(1)

  hnswlibIndexer.indexDatamart(datamartFilename, indexFilename)
}
