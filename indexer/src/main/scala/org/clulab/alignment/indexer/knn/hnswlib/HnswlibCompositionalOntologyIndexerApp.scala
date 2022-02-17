package org.clulab.alignment.indexer.knn.hnswlib

object HnswlibCompositionalOntologyIndexerApp extends App {

  def run(conceptIndexFilename: String, processIndexFilename: String, propertyIndexFilename: String,
      compositionalFilenameOpt: Option[String] = None): Unit = {
    val hnswlibIndexer = new HnswlibIndexer()

    hnswlibIndexer.indexCompositionalOntology(conceptIndexFilename, processIndexFilename, propertyIndexFilename, compositionalFilenameOpt)
  }

  run(args(0), args(1), args(2))
}
