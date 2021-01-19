package org.clulab.alignment.scraper

import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ujson.Value

import java.nio.charset.StandardCharsets

class SuperMaasCubeScraper(baseUrl: String, createdSince: String = "") extends SuperMaasSingleScraper(baseUrl, createdSince) {

  def scrape(tsvWriter: TsvWriter): Unit = {
    val parameters =
        if (createdSince.nonEmpty) "created_since=" + encode(createdSince)
        else ""
    val cubesUrl = s"$baseUrl/cubes" + (if (parameters.isEmpty) "" else "?" + parameters)
    val datasetsText = requests.get(cubesUrl).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset: Value =>
      val datasetId = dataset("id").num.toInt.toString // Generated during registering
      val created = dataset("created").str // Generated during registering

      SuperMaasCubeScraper.logger.info(s"Scraping SuperMaaS_Cube datasetId $datasetId created $created")

      val datasetName = stringElse(dataset, "name", "") // Optional
      val datasetDescription = dataset("description").str // Compulsory
      val datasetUrl = s"$baseUrl/cubes/$datasetId"
      val parameters = arrElseEmpty(dataset, "parameters") // Compulsory, but null if empty
      val independentVars = arrElseEmpty(dataset, "independent_vars") // Compulsory, but null if empty
      val dependentVars = arrElseEmpty(dataset, "dependent_vars") // Compulsory, but null if empty

      val variableContext = VariableContext(SuperMaasCubeScraper.datamartId, datasetId, datasetName, datasetDescription, datasetUrl)

      scrapeLongVariables(tsvWriter, variableContext, parameters)
      scrapeShortVariables(tsvWriter, variableContext, independentVars)
      scrapeShortVariables(tsvWriter, variableContext, dependentVars)
    }
  }
}

object SuperMaasCubeScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  // Notice how this differs from other versions with the capital S on the end.
  val datamartId = "SuperMaaS_Cube"
}
