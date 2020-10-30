package org.clulab.alignment.webapp

import java.io.File
import java.nio.file.Files

import org.clulab.alignment.indexer.knn.hnswlib.HnswlibLocations
import org.clulab.alignment.indexer.lucene.LuceneLocations
import org.clulab.alignment.scraper.ScraperLocations

class IndexerLocations(val index: Int = 0, val baseDir: String = WebappLocations.baseDir, val baseFile: String = WebappLocations.baseFile) {
  val parentDir: String = s"$baseDir/$baseFile$index"

  val scraperLocations: ScraperLocations = new ScraperLocations(index, baseDir, baseFile)
  val hnswlibLocations: HnswlibLocations = new HnswlibLocations(index, baseDir, baseFile)
  val luceneLocations: LuceneLocations = new LuceneLocations(index, baseDir, baseFile)

  def mkdirs(): Unit = {
    val file = new File(parentDir)
    if (!file.exists)
      file.mkdirs()
  }

  def delete(): Unit = {
    FileUtils.rmdir(parentDir)
  }
}
