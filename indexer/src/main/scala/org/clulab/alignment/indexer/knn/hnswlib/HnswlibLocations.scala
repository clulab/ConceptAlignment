package org.clulab.alignment.indexer.knn.hnswlib

class HnswlibLocations(val index: Int = 0, baseDir: String = HnswlibLocations.baseDir, baseFile: String = HnswlibLocations.baseFile) extends HnswlibLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(HnswlibLocations.datamartName)
  val datamartIndexFilename: String = mkFilename(HnswlibLocations.datamartIndexName)
  val flatOntologyIndexFilename: String = s"$baseDir/${HnswlibLocations.ontologyIndexName}"

  val conceptIndexFilename: String = s"$baseDir/${HnswlibLocations.conceptIndexName}"
  val processIndexFilename: String = s"$baseDir/${HnswlibLocations.processIndexName}"
  val propertyIndexFilename: String = s"$baseDir/${HnswlibLocations.propertyIndexName}"

  val gloveIndexFilename: String = s"$baseDir/${HnswlibLocations.gloveIndexName}"

  def next: HnswlibLocations = new HnswlibLocations(index + 1, baseDir, baseFile)
}

object HnswlibLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "datamarts.tsv"
  val datamartIndexName = "hnswlib-datamart.idx"
  val ontologyIndexName = "hnswlib-wm_flattened.idx"

  val conceptIndexName = "hnswlib-concept.idx"
  val processIndexName = "hnswlib-process.idx"
  val propertyIndexName = "hnswlib-property.idx"

  val gloveIndexName = "hnswlib-glove.idx"

  val defaultLocations = new HnswlibLocations()
}
