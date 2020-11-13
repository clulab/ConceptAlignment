package org.clulab.alignment.indexer.knn.hnswlib

import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex

class HnswlibIndexerApp(hnswlibLocations: HnswlibLocationsTrait) {
  val hnswlibIndexer = new HnswlibIndexer()

  def run(ontology: Boolean = false, glove: Boolean = false): DatamartIndex.Index = {
    // Control these through boolean arguments.
    // hnswlibIndexer.indexSample()
    val datamartIndex = hnswlibIndexer.indexDatamart(hnswlibLocations.datamartFilename, hnswlibLocations.datamartIndexFilename)
    if (ontology)
      hnswlibIndexer.indexOntology(hnswlibLocations.ontologyIndexFilename)
    if (glove)
      hnswlibIndexer.indexGlove(hnswlibLocations.gloveIndexFilename)

    datamartIndex
  }
}

class StaticHnswlibLocations(
  val datamartFilename: String,
  val datamartIndexFilename: String,
  val ontologyIndexFilename: String,
  val gloveIndexFilename: String
) extends HnswlibLocationsTrait {
}

object HnswlibIndexerApp extends App {
  val datamartFilename = args(0)
  val datamartIndexFilename = args(1)
  val ontologyIndexFilename = args(2)
  val gloveIndexFilename = args(3)
  val glove = args(4).toBoolean

  new HnswlibIndexerApp(new StaticHnswlibLocations(datamartFilename, datamartIndexFilename, ontologyIndexFilename, gloveIndexFilename)).run(glove)
}
