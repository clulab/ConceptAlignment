package org.clulab.alignment.scraper

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ujson.Value

// The value for createSince has to match the format coming from SuperMaaS.
// In general, the value should be the greatest value seen to date so that
// each dataset delivered has a value greater that it.
class SuperMaasScraper(baseUrl: String, createdSince: String = "") extends DatamartScraper {

  def encode(parameter: String) = URLEncoder.encode(createdSince, StandardCharsets.UTF_8.toString)

  def stringElse(value: Value, key: String, consolation: String): String = {
    if (value.obj.get(key).nonEmpty)
      value(key).str
    else
      consolation
  }

  def arrElseEmpty(value: Value, key: String): IndexedSeq[Value] = {
    if (value.obj.get(key).nonEmpty && ! value(key).isNull)
      value(key).arr.toIndexedSeq
    else
      IndexedSeq.empty[Value]
  }

  def scrape(tsvWriter: TsvWriter): Unit = {
    val parameters =
        if (createdSince.nonEmpty) "created_since=" + encode(createdSince)
        else ""
    val modelsUrl = s"$baseUrl/models" + (if (parameters.isEmpty) "" else "?" + parameters)
    val datasetsText = requests.get(modelsUrl).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset: Value =>
      val datasetId = dataset("id").num.toInt.toString // Generated during registering
      val datasetName = stringElse(dataset, "name", "") // Optional
      val datasetDescription = dataset("description").str // Compulsory
      val datasetUrl = s"$baseUrl/models/$datasetId"
      val created = dataset("created").str // Generated during registering
      val parameters = arrElseEmpty(dataset, "parameters") // Compulsory, but null if empty

      SuperMaasScraper.logger.info(s"Scraping SuperMaaS datasetId $datasetId created $created")

      parameters.foreach { parameter =>
        val parameterName = parameter("name").str // Compulsory if exposed
        val parameterDescription = stringElse(parameter, "description", "") // Optional

        val variableId = parameterName
        val variableName = parameterName
        val variableDescription = parameterDescription

        tsvWriter.println(SuperMaasScraper.datamartId, datasetId, datasetName, datasetDescription, datasetUrl, variableId, variableName, variableDescription)
      }
    }
  }
}

object SuperMaasScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  // Notice how this differs from other versions with the capital S on the end.
  val datamartId = "SuperMaaS"
  val stanza = "SuperMaasScraper"
  val urlKey = s"$stanza.url"

  def fromConfig(config: Config): SuperMaasScraper = {
    val baseUrl = config.getString(urlKey)
    val createdSince = config.getString(s"$stanza.createdSince")

    new SuperMaasScraper(baseUrl, createdSince)
  }

  def setUrl(config: Config, url: String): Config = {
    config.withValue(urlKey, ConfigValueFactory.fromAnyRef(url))
  }
}
