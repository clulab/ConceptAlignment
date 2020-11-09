package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibDatamartIndexerApp extends App {
  val datamartFilename = args(0)
  val indexFilename = args(1)

  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexDatamart(datamartFilename, indexFilename)
}
