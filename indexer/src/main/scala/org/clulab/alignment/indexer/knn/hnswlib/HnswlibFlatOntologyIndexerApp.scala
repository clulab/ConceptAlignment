package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibFlatOntologyIndexerApp extends App {

  def run(indexFilename: String): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexFlatOntology(indexFilename)
  }

  run(args(0))
}
