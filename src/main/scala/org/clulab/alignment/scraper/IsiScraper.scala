package org.clulab.alignment.scraper

import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import org.clulab.alignment.utils.PropertiesBuilder
import org.clulab.wm.eidos.utils.TsvWriter
import requests.RequestAuth.Basic

class IsiScraper(baseUrl: String, username: String, password: String) extends DatamartScraper {
  protected val auth = new Basic(username, password)

  def scrape(tsvWriter: TsvWriter): Unit = {
    val datasetsUrl = s"$baseUrl/metadata/datasets"
    val datasetsText = requests.get(datasetsUrl, auth).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset =>
      val datasetId = dataset("dataset_id").str
      val datasetName = dataset("name").str
      val datasetDescription = dataset("description").str
      val datasetUrl = dataset("url").str
      val variablesUrl = s"$baseUrl/metadata/datasets/$datasetId/variables"
      val variablesText = requests.get(variablesUrl, auth).text(StandardCharsets.UTF_8)
      val variables = ujson.read(variablesText).arr.toIndexedSeq

      println(variablesText)
      println()
      variables.foreach { variable =>
        val variableId = variable("variable_id").str
        val variableName = variable("name").str
        val variableDescription = variable("description").str

        tsvWriter.println(IsiScraper.datamartId, datasetId, datasetName, datasetDescription, datasetUrl, variableId, variableName, variableDescription)
      }
    }
  }
}

object IsiScraper {
  val datamartId = "ISI"

  def fromConfig(config: Config): IsiScraper = {
    val baseUrl = config.getString("IsiScraper.url")
    val login = config.getString("IsiScraper.login")
    val propertiesBuilder = PropertiesBuilder.fromFile(login)
    val username = propertiesBuilder.getProperty("username").get
    val password = propertiesBuilder.getProperty("password").get

    new IsiScraper(baseUrl, username, password)
  }
}