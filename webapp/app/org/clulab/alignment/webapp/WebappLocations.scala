package org.clulab.alignment.webapp

import java.io.File

import org.clulab.alignment.Locations
import org.clulab.alignment.LocationsTrait

class WebappLocations extends Locations {
    val baseLocations = Locations.defaultLocations
    val fileAndIndexOpt = FileUtils.findFileAndIndex(baseLocations.baseDir, baseLocations.baseFile)
    require(fileAndIndexOpt.isDefined)
    val locations = new Locations(fileAndIndexOpt.get._2)


    // Perhaps contains SingleKnnApp locations
    // and other kinds of locations
    // One for the indexing, for example
}
