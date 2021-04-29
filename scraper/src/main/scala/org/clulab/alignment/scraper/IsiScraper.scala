package org.clulab.alignment.scraper

import java.nio.charset.StandardCharsets
import com.typesafe.config.Config
import org.clulab.alignment.utils.PropertiesBuilder
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import requests.RequestAuth.Basic

import scala.collection.mutable.{HashSet => MutableHashSet}
import scala.util.control.NonFatal

class IsiScraper(baseUrl: String, username: String, password: String) extends DatamartScraper {
  protected val auth = new Basic(username, password)
  protected val readTimeout = 60000

  def scrape(tsvWriter: TsvWriter): Unit = {
    val datasetsUrl = s"$baseUrl/metadata/datasets"
    val datasetsText = requests.get(datasetsUrl, auth = auth, readTimeout = readTimeout).text(StandardCharsets.UTF_8)
    val datasets = ujson.read(datasetsText).arr.toIndexedSeq

    datasets.foreach { dataset =>
      val datasetId = dataset("dataset_id").str

      IsiScraper.logger.info(s"Scriaping ISI datasetId $datasetId")

      val datasetName = dataset("name").str
      val datasetDescription = dataset("description").str
      val datasetUrl = dataset("url").str
      val variablesUrl = s"$baseUrl/metadata/datasets/$datasetId/variables"
      val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty
      // Sometimes the variable is not there.
      try {
        val variablesText = requests.get(variablesUrl, auth = auth, readTimeout = readTimeout).text(StandardCharsets.UTF_8)
        val variables = ujson.read(variablesText).arr.toIndexedSeq

        variables.foreach { variable =>
          val variableId = variable("variable_id").str
          // The name is sometimes missing.
          val variableName = variable.obj.getOrElse("name", IsiScraper.blankJson).str
          val variableDescription = variable("description").str
          val doubleId = (datasetId, variableId)

          if (doubleIds.contains(doubleId))
            IsiScraper.logger.error(s"The ISI (dataset_id, variable_id) of ($datasetId, $variableId) is duplicated and skipped.")
          else {
            doubleIds.add(doubleId)
            tsvWriter.println(
              IsiScraper.datamartId,
              datasetId,
              datasetName,
              "[]", // tags
              datasetDescription,
              datasetUrl,
              variableId,
              variableName,
              "[]", // tags
              variableDescription
            )
          }
        }
      }
      catch {
        case NonFatal(throwable) => println(throwable.getMessage)
      }
    }
  }
}

object IsiScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  // Notice how this differs from other versions with the capital S on the end.
  val datamartId = "ISI"
  val blankJson = ujson.Str("")

  def fromConfig(config: Config): IsiScraper = {
    val baseUrl = config.getString("IsiScraper.url")
    val login = config.getString("IsiScraper.login")
    val propertiesBuilder = PropertiesBuilder.fromFile(login)
    val username = propertiesBuilder.getProperty("username").get
    val password = propertiesBuilder.getProperty("password").get

    new IsiScraper(baseUrl, username, password)
  }
}