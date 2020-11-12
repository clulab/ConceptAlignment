package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibOntologyIndexerApp extends App {
//  val datamartFilename = args(0)
//  val indexFilename = args(1)

  val hnswlibIndexer = new HnswlibIndexer()
  hnswlibIndexer.indexOntology()
//  hnswlibIndexer.indexDatamart(datamartFilename, indexFilename)
}
