package org.clulab.alignment.scraper

class ScraperLocations(val index: Int = 0, baseDir: String = ScraperLocations.baseDir, baseFile: String = ScraperLocations.baseFile) extends ScraperLocationsTrait {

  def mkFilename(name: String): String = s"$baseDir/$baseFile$index/$name"

  val datamartFilename: String = mkFilename(ScraperLocations.datamartName)

  def next: ScraperLocations = new ScraperLocations(index + 1, baseDir, baseFile)
}

object ScraperLocations {
  val baseDir = ".."
  val baseFile = "index_"

  val datamartName = "datamarts.tsv"

  val defaultLocations = new ScraperLocations()
}
