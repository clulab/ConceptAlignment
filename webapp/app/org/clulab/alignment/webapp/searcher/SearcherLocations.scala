package org.clulab.alignment.webapp

import org.clulab.alignment.searcher.knn.KnnLocations

class SearchLocations(index: Int = 0, baseDir: String = WebappLocations.baseDir, baseFile: String = WebappLocations.baseFile)
    extends KnnLocations(index, baseDir, baseFile)

class AutoKnnLocations() extends SearchLocations(AutoKnnLocations.getBaseIndex) {
}

object AutoKnnLocations {

  def getBaseIndex: Int = getBaseIndex(WebappLocations.baseDir, WebappLocations.baseFile)

  def getBaseIndex(baseDir: String, baseFile: String): Int = {
    val baseLocations = new KnnLocations(baseDir = WebappLocations.baseDir, baseFile = WebappLocations.baseFile)
    val fileAndIndexOpt = FileUtils.findFileAndIndex(baseLocations.baseDir, baseLocations.baseFile)

    require(fileAndIndexOpt.isDefined)
    fileAndIndexOpt.get._2
  }

  def getNextIndex: Int = {
    try {
      val locations = new AutoKnnLocations()
      locations.index + 1
    }
    catch {
      case _: Throwable =>
        val locations = new KnnLocations()
        locations.index
    }
  }
}


