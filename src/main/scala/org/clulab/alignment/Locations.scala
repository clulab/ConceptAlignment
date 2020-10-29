package org.clulab.alignment

class Locations(var index: Int = 0) extends LocationsTrait {
  val baseDir = ".."
  val baseFile = "index_"

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartName = "hnswlib-datamart.idx"
  val luceneName = "lucene-datamart"

  val datamartFilename: String = mkFilename(datamartName)
  val gloveFilename: String = s"$baseDir/hnswlib-glove.idx"
  val luceneDirname: String = mkFilename(luceneName)

  def next: Locations = new Locations(index + 1)
}

object Locations {
  val defaultLocations = new Locations()
}
