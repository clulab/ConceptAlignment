package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibDatamartIndexerApp extends App {
  val hnswlibIndexer = new HnswlibIndexer()
  
  hnswlibIndexer.indexDatamart(args(0))
}
