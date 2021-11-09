package org.clulab.alignment.builder

import com.typesafe.config.{Config, ConfigFactory}
import org.clulab.alignment.indexer.knn.hnswlib.{HnswlibCompositionalOntologyIndexerApp, HnswlibDatamartIndexerApp, HnswlibFlatOntologyIndexerApp, HnswlibGloveIndexerApp}
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.{DatamartScraper, DojoFileScraper, IsiScraper, ScraperApp, StaticScraperLocations}

import java.io.File

// TODO Do this twice, once for glove for the 0 version?

object BuilderApp extends App {
  val baseDirName = "../builder"
  val indexDirName = baseDirName + "/index_1"
  val datamartFileName = indexDirName + "/datamarts.tsv"

  // $ # Make a directory to contain the indexes of the form ../index_# where the number might be 0.
  // $ mkdir ../index_0
  val file = new File(indexDirName)
  if (!file.exists)
    file.mkdirs()

  def indexGlove(): Unit = {
    // $ # Run this one just once because it takes a long time and glove shouldn't change.  It doesn't go into ../Index_0.
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibGloveIndexerApp ../hnswlib-glove.idx"
    val gloveFileName = baseDirName + "/hnswlib-glove.idx"
    HnswlibGloveIndexerApp.run(gloveFileName)
  }

  def scrapeDatamarts(): Unit = {
    // $ # Scrape the datamarts, all of them if necessary.  Credentials are required.
    // $ sbt "scraper/runMain org.clulab.alignment.scraper.ScraperApp ../index_0/datamarts.tsv"
    // $ # For testing, sometimes SuperMaaS is only needed.
    // $ sbt "scraper/runMain org.clulab.alignment.scraper.SuperMaasScraperApp ../index_0/datamarts.tsv"

    // ScraperApp.run(datamartFileName) // This would use all the scrapers.
    val config = ConfigFactory.load
    // val isiScraper = IsiScraper.fromConfig(config)
    val dojoFileScraper = DojoFileScraper.fromConfig(config)
    new ScraperApp(new StaticScraperLocations(datamartFileName)).run(Seq(dojoFileScraper))
  }

  def indexDatamarts(): Unit = {
    // $ # Run these each time the datamarts have changed.
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp ../index_0/datamarts.tsv ../index_0/hnswlib-datamart.idx"
    val datamartIndexFileName = indexDirName + "/hnswlib-datamart.idx"
    HnswlibDatamartIndexerApp.run(datamartFileName, datamartIndexFileName)
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

  def luceneIndexDatamarts(): Unit = {
    // $ sbt "indexer/runMain org.clulab.alignment.indexer.lucene.LuceneIndexerApp ../index_0/datamarts.tsv ../index_0/lucene-datamart"
    val luceneIndexDirName = indexDirName + "/lucene-datamart"
    LuceneIndexerApp.run(datamartFileName, luceneIndexDirName)
  }

//  indexGlove()
//  scrapeDatamarts()
  indexDatamarts()
//  indexOntologies()
//  luceneIndexDatamarts()
}
