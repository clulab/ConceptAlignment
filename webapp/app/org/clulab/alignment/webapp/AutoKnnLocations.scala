package org.clulab.alignment.webapp

import org.clulab.alignment.searcher.knn.KnnLocations

class AutoKnnLocations() extends KnnLocations(AutoKnnLocations.getBaseIndex()) {
}

object AutoKnnLocations {

  def getBaseIndex(): Int = {
    val baseLocations = KnnLocations.defaultLocations
    val fileAndIndexOpt = FileUtils.findFileAndIndex(baseLocations.baseDir, baseLocations.baseFile)
    require(fileAndIndexOpt.isDefined)

    fileAndIndexOpt.get._2
  }
}


