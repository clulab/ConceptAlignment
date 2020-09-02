package org.clulab.alignment.scraper

import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import org.clulab.alignment.utils.TsvWriter
import ujson.Value

class SuperMaasScraper(baseUrl: String) extends DatamartScraper {

  def scrape(tsvWriter: TsvWriter): Unit = {
    val modelsUrl = s"$baseUrl/models"
    val datasetsText = requests.get(modelsUrl).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset =>
      val datasetId = dataset("id").num.toInt.toString
      val datasetName = dataset("label").str
      val datasetDescription = dataset("model_description").str
      val datasetUrl = s"$baseUrl/models/$datasetId"
      // Parameters might be null
      val parameters = dataset("parameters").arr.map(_.str).toArray
      val parameterDescriptions = dataset("parameter_descriptions").arr.map(_.str).toArray

      require(parameters.length == parameterDescriptions.length)

      parameters.zip(parameterDescriptions).foreach { case (parameter, parameterDescription) =>
        val variableId = parameter
        val variableName = parameter
        val variableDescription = parameterDescription

        tsvWriter.println(SuperMaasScraper.datamartId, datasetId, datasetName, datasetDescription, datasetUrl, variableId, variableName, variableDescription)
      }
    }
  }
}

object SuperMaasScraper {
  val datamartId = "SuperMaas"

  def fromConfig(config: Config): SuperMaasScraper = {
    val baseUrl = config.getString("SuperMaasScraper.url")

    new SuperMaasScraper(baseUrl)
  }
}