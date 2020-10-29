package org.clulab.alignment.webapp

import org.clulab.alignment.Locations

class AutoLocations() extends Locations(AutoLocations.getBaseIndex()) {
}

object AutoLocations {

  def getBaseIndex(): Int = {
    val baseLocations = Locations.defaultLocations
    val fileAndIndexOpt = FileUtils.findFileAndIndex(baseLocations.baseDir, baseLocations.baseFile)
    require(fileAndIndexOpt.isDefined)

    fileAndIndexOpt.get._2
  }
}


