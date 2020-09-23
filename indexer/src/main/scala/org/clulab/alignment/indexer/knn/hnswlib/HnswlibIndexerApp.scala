package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibIndexerApp extends App {
  val hnswlibIndexer = new HnswlibIndexer()

  hnswlibIndexer.indexSample()
  hnswlibIndexer.indexOntology()
  hnswlibIndexer.indexGlove()
  hnswlibIndexer.indexDatamart("../datamarts.tsv")
}
