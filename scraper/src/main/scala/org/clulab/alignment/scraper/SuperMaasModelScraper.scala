package org.clulab.alignment.scraper

import java.nio.charset.StandardCharsets

import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ujson.Value

class SuperMaasModelScraper(baseUrl: String, createdSince: String = "") extends SuperMaasSingleScraper(baseUrl, createdSince) {

  def scrape(tsvWriter: TsvWriter): Unit = {
    val parameters =
        if (createdSince.nonEmpty) "created_since=" + encode(createdSince)
        else ""
    val modelsUrl = s"$baseUrl/models" + (if (parameters.isEmpty) "" else "?" + parameters)
    val datasetsText = requests.get(modelsUrl).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset: Value =>
      val datasetId = dataset("id").num.toInt.toString // Generated during registering
      val created = dataset("created").str // Generated during registering

      SuperMaasModelScraper.logger.info(s"Scraping SuperMaaS_Model datasetId $datasetId created $created")

      val datasetName = stringElse(dataset, "name", "") // Optional
      val datasetTags = arrElseEmpty(dataset, "tags").map(_.str)
      val datasetDescription = dataset("description").str // Compulsory
      val datasetUrl = s"$baseUrl/models/$datasetId"
      val parameters = arrElseEmpty(dataset, "parameters") // Compulsory, but null if empty
      val cubesOutputs = arrElseEmpty(dataset, "outputs") // Compulsory, but null if empty

      val variableContext = VariableContext(
        SuperMaasModelScraper.datamartId,
        datasetId,
        datasetName,
        datasetTags,
        datasetDescription,
        datasetUrl
      )

      scrapeLongVariables(tsvWriter, variableContext, parameters)
      cubesOutputs.foreach { cubeOutputs =>
        scrapeLongVariables(tsvWriter, variableContext, cubeOutputs.arr)
      }
    }
  }
}

object SuperMaasModelScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  // Notice how this differs from other versions with the capital S on the end.
  val datamartId = "SuperMaaS_Model"
}
