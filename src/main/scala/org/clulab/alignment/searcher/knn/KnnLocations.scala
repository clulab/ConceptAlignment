package org.clulab.alignment.searcher.knn

class KnnLocations(val index: Int = 0, val baseDir: String = KnnLocations.baseDir, val baseFile: String = KnnLocations.baseFile) extends KnnLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(KnnLocations.datamartName)
  val    gloveFilename: String = s"$baseDir/${KnnLocations.gloveName}"
  val    luceneDirname: String = mkFilename(KnnLocations.luceneName)

  def next: KnnLocations = new KnnLocations(index + 1, baseDir, baseFile)
}

object KnnLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "hnswlib-datamart.idx"
  val    gloveName = "hnswlib-glove.idx"
  val   luceneName = "lucene-datamart"

  val defaultLocations = new KnnLocations()
}
