package org.clulab.alignment.searcher.knn

class KnnLocations(val index: Int = 0, val baseDir: String = KnnLocations.baseDir, val baseFile: String = KnnLocations.baseFile) extends KnnLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(KnnLocations.datamartName)
  val ontologyFilename: String = s"$baseDir/${KnnLocations.ontologyName}"

  val  conceptFilename: String = s"$baseDir/${KnnLocations.conceptName}"
  val  processFilename: String = s"$baseDir/${KnnLocations.processName}"
  val propertyFilename: String = s"$baseDir/${KnnLocations.propertyName}"

  val    gloveFilename: String = s"$baseDir/${KnnLocations.gloveName}"
  val    luceneDirname: String = mkFilename(KnnLocations.luceneName)

  def next: KnnLocations = new KnnLocations(index + 1, baseDir, baseFile)
}

object KnnLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "hnswlib-datamart.idx"
  val ontologyName = "hnswlib-wm_flattened.idx"

  val  conceptName = "hnswlib-concept.idx"
  val  processName = "hnswlib-process.idx"
  val propertyName = "hnswlib-property.idx"

  val    gloveName = "hnswlib-glove.idx"
  val   luceneName = "lucene-datamart"

  val defaultLocations = new KnnLocations()
}
