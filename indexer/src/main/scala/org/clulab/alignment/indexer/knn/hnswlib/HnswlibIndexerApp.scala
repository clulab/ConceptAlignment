package org.clulab.alignment.indexer.knn.hnswlib

import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex

class HnswlibIndexerApp(hnswlibLocations: HnswlibLocationsTrait) {
  val hnswlibIndexer = new HnswlibIndexer()

  def run(flatOntology: Boolean = false, compositionalOntology: Boolean = false, glove: Boolean = false): DatamartIndex.Index = {
    // Control these through boolean arguments.
    // hnswlibIndexer.indexSample()
    val datamartIndex = hnswlibIndexer.indexDatamart(hnswlibLocations.datamartFilename, hnswlibLocations.datamartIndexFilename)
    if (flatOntology)
      hnswlibIndexer.indexFlatOntology(hnswlibLocations.flatOntologyIndexFilename)
    if (compositionalOntology)
      hnswlibIndexer.indexCompositionalOntology(hnswlibLocations.conceptIndexFilename, hnswlibLocations.processIndexFilename, hnswlibLocations.propertyIndexFilename)
    if (glove)
      hnswlibIndexer.indexGlove(hnswlibLocations.gloveIndexFilename)

    datamartIndex
  }
}

class StaticHnswlibLocations(
  val datamartFilename: String,
  val datamartIndexFilename: String,
  val flatOntologyIndexFilename: String,

  val conceptIndexFilename: String,
  val processIndexFilename: String,
  val propertyIndexFilename: String,

  val gloveIndexFilename: String
) extends HnswlibLocationsTrait {
}

object HnswlibIndexerApp extends App {
  val datamartFilename = args(0)
  val datamartIndexFilename = args(1)
  val flatOntologyIndexFilename = args(2)

  val conceptIndexFilename = args(3)
  val processIndexFilename = args(4)
  val propertyIndexFilename = args(5)

  val gloveIndexFilename = args(6)
  val glove = args(4).toBoolean

  new HnswlibIndexerApp(new StaticHnswlibLocations(datamartFilename, datamartIndexFilename, flatOntologyIndexFilename,
      conceptIndexFilename, processIndexFilename, propertyIndexFilename, gloveIndexFilename)).run(glove = glove)
}
