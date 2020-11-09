package org.clulab.alignment.indexer.knn.hnswlib

class HnswlibLocations(val index: Int = 0, baseDir: String = HnswlibLocations.baseDir, baseFile: String = HnswlibLocations.baseFile) extends HnswlibLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(HnswlibLocations.datamartName)
  val datamartIndexFilename: String = mkFilename(HnswlibLocations.datamartIndexName)
  val gloveIndexFilename: String = s"$baseDir/${HnswlibLocations.gloveIndexName}"

  def next: HnswlibLocations = new HnswlibLocations(index + 1, baseDir, baseFile)
}

object HnswlibLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "datamarts.tsv"
  val datamartIndexName = "hnswlib-datamart.idx"
  val gloveIndexName = "hnswlib-glove.idx"

  val defaultLocations = new HnswlibLocations()
}
