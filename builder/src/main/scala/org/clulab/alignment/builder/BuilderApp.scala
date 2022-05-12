package org.clulab.alignment.builder

import com.typesafe.config.ConfigFactory
import org.clulab.alignment.indexer.knn.hnswlib.{HnswlibCompositionalOntologyIndexerApp, HnswlibDatamartIndexerApp, HnswlibFlatOntologyIndexerApp, HnswlibGloveIndexerApp}
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.{DojoRestScraper, ScraperApp, StaticScraperLocations}

import java.io.File

object BuilderApp extends App {

  def ensureDirExists(dirName: String): String = {
    val file = new File(dirName)
    if (!file.exists)
      file.mkdirs()

    dirName
  }

  val baseDirName = "../builder"
  val datamartFileName = "/datamarts.tsv"
  // $ # Make a directory to contain the indexes of the form ../index_# where the number might be 0.
  val indexDirName0 = ensureDirExists(baseDirName + "/index_0")
  val indexDirName1 = ensureDirExists(baseDirName + "/index_1")

  val datamartFileName0 = indexDirName0 + datamartFileName
  val datamartFileName1 = indexDirName1 + datamartFileName

  def indexGlove(countOpt: Option[Int] = None): Unit = {
    // $ # Run this one just once because it takes a long time and glove shouldn't change.  It doesn't go into ../Index_0.
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibGloveIndexerApp ../hnswlib-glove.idx"
    val gloveFileName = baseDirName + "/hnswlib-glove.idx"
    HnswlibGloveIndexerApp.run(gloveFileName, countOpt)
  }

  def scrapeDatamarts0(): Unit = {
    // Make an empty file for indexing just so that files are generated.
    new ScraperApp(new StaticScraperLocations(datamartFileName0)).run(Seq.empty)
  }

  def indexDatamarts0(): Unit = {
    // $ # Run these each time the datamarts have changed.
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp ../index_0/datamarts.tsv ../index_0/hnswlib-datamart.idx"
    val datamartIndexFileName = indexDirName0 + "/hnswlib-datamart.idx"
    HnswlibDatamartIndexerApp.run(datamartFileName0, datamartIndexFileName)
  }

  def luceneIndexDatamarts0(): Unit = {
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.lucene.LuceneIndexerApp ../index_0/datamarts.tsv ../index_0/lucene-datamart"
    val luceneIndexDirName = indexDirName0 + "/lucene-datamart"
    LuceneIndexerApp.run(datamartFileName0, luceneIndexDirName)
  }

  def scrapeDatamarts1(): Unit = {
    // $ # Scrape the datamarts, all of them if necessary.  Credentials are required.
    // $ sbt "scraper/runMain org.clulab.alignment.scraper.ScraperApp ../index_0/datamarts.tsv"
    // $ # For testing, sometimes SuperMaaS is only needed.
    // $ sbt "scraper/runMain org.clulab.alignment.scraper.SuperMaasScraperApp ../index_0/datamarts.tsv"

    // ScraperApp.run(datamartFileName) // This would use all the scrapers.
    val config = ConfigFactory.load
    val scrapers = Seq(
      // IsiScraper.fromConfig(config),
      DojoRestScraper.fromConfig(config)
    )
    new ScraperApp(new StaticScraperLocations(datamartFileName1)).run(scrapers)
  }

  def indexDatamarts1(): Unit = {
    // $ # Run these each time the datamarts have changed.
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp ../index_0/datamarts.tsv ../index_0/hnswlib-datamart.idx"
    val datamartIndexFileName = indexDirName1 + "/hnswlib-datamart.idx"
    HnswlibDatamartIndexerApp.run(datamartFileName1, datamartIndexFileName)
  }

  def luceneIndexDatamarts1(): Unit = {
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.lucene.LuceneIndexerApp ../index_0/datamarts.tsv ../index_0/lucene-datamart"
    val luceneIndexDirName = indexDirName1 + "/lucene-datamart"
    LuceneIndexerApp.run(datamartFileName1, luceneIndexDirName)
  }

  def indexOntologies(): Unit = {
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibFlatOntologyIndexerApp ../hnswlib-wm_flattened.idx"
    val flatOntologyIndexFileName = baseDirName + "/hnswlib-wm_flattened.idx"
    HnswlibFlatOntologyIndexerApp.run(flatOntologyIndexFileName)

    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibCompositionalOntologyIndexerApp ../hnswlib-concept.idx ../hnswlib-process.idx ../hnswlib-property.idx"
    val conceptIndexFileName = baseDirName + "/hnswlib-concept.idx"
    val processIndexFileName = baseDirName + "/hnswlib-process.idx"
    val propertyIndexFileName = baseDirName + "/hnswlib-property.idx"
    HnswlibCompositionalOntologyIndexerApp.run(conceptIndexFileName, processIndexFileName, propertyIndexFileName)
  }

  scrapeDatamarts0()
  indexDatamarts0()
  luceneIndexDatamarts0()

  scrapeDatamarts1()
  indexDatamarts1()
  luceneIndexDatamarts1()

  indexOntologies()
  indexGlove(None) // Some(1000))
}
