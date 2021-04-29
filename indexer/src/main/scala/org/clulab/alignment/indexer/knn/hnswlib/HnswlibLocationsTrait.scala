package org.clulab.alignment.indexer.knn.hnswlib

trait HnswlibLocationsTrait {
  val datamartFilename: String
  val datamartIndexFilename: String
  val flatOntologyIndexFilename: String

  val conceptIndexFilename: String
  val processIndexFilename: String
  val propertyIndexFilename: String

  val gloveIndexFilename: String
}
