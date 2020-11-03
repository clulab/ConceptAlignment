package org.clulab.alignment.indexer.lucene

class LuceneLocations(val index: Int = 0, baseDir: String = LuceneLocations.baseDir, baseFile: String = LuceneLocations.baseFile) extends LuceneLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(LuceneLocations.datamartName)
  val luceneDirname: String = mkFilename(LuceneLocations.luceneName)

  def next: LuceneLocations = new LuceneLocations(index + 1, baseDir, baseFile)
}

object LuceneLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "datamarts.tsv"
  val luceneName = "lucene-datamart"

  val defaultLocations = new LuceneLocations()
}
