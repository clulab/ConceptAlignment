package org.clulab.alignment.webapp.utils

import org.clulab.alignment.webapp.WebappLocations

class AutoLocations {
  val baseDir: String = WebappLocations.baseDir
  val baseFile: String = WebappLocations.baseFile
  val index: Int = AutoLocations
      .getBaseIndexOpt(baseDir, baseFile)
      .getOrElse(throw new Exception("No index was found."))
}

object AutoLocations {

  def getBaseIndexOpt: Option[Int] = getBaseIndexOpt(WebappLocations.baseDir, WebappLocations.baseFile)

  def getBaseIndexOpt(baseDir: String, baseFile: String): Option[Int] = {
    val fileAndIndexOpt = FileUtils.findFileAndIndex(baseDir, baseFile)

    fileAndIndexOpt.map(_._2)
  }
}



