package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.utils.PropertiesBuilder
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import requests.RequestAuth.Basic

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.collection.mutable.{HashSet => MutableHashSet}
import scala.util.control.NonFatal

class DojoRestScraper(baseUrl: String, username: String, password: String) extends DojoScraper {
  protected val auth = new Basic(username, password)
  protected val readTimeout = 120000
  protected val connectTimeout = 120000
  val modelsUrl = new ModelsUrl(baseUrl)
  val indicatorsUrl = new IndicatorsUrl(baseUrl)

  def getJVal(datasetsUrl: String): ujson.Value = {
    val json = requests.get(datasetsUrl, auth = auth, readTimeout = readTimeout, connectTimeout = connectTimeout).text(StandardCharsets.UTF_8)
    ujson.read(json)
  }

  def scrapeModels(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    @tailrec
    def loop(jVal: ujson.Value): Unit = {
      val jObj = jVal.obj
      val hits = jObj("hits").num.toInt
      val scrollIdOpt = jObj
          .get("scroll_id")
          // This is necessary because it isn't real null but instead ujson.Null.
          .flatMap { stringOrNull => if (stringOrNull.isNull) None else Some(stringOrNull) }
          .map(_.str)
      val results = jObj("results").arr
      val size = results.size

      results.foreach { result =>
        val jObj = result.obj
        val dojoDocument = new ModelDocument(jObj)
        val datasetId = dojoDocument.id

        if (dojoDocument.skip)
          DojoRestScraper.logger.info(s"Skip scraping deprecated DOJO model with datasetId $datasetId")
        else {
          DojoRestScraper.logger.info(s"Scraping DOJO model with datasetId $datasetId")
          dojoDocument.parameters.map { parameter =>
            writeDojoRecord(dojoDocument, parameter, tsvWriter, doubleIds, DojoRestScraper.logger)
          }
          dojoDocument.outputs.foreach { dojoOutput =>
            writeDojoRecord(dojoDocument, dojoOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
          }
//        dojoDocument.qualifierOutputsOpt.map { qualifierOutputs =>
//          qualifierOutputs.foreach { qualifierOutput =>
//            writeDojoRecord(dojoDocument, qualifierOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
//          }
//        }
        }
      }

      if (scrollIdOpt.isDefined && size >= modelsUrl.size) {
        val jVal = getJVal(modelsUrl.getTailUrl(scrollIdOpt.get))
        loop(jVal)
      }
    }

    try {
      val jVal = getJVal(modelsUrl.getHeadUrl())
      loop(jVal)
    }
    catch {
      case NonFatal(throwable) => println(throwable.getMessage)
    }
  }

  def scrapeIndicators(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    @tailrec
    def loop(jVal: ujson.Value): Unit = {
      val jObj = jVal.obj
      val hits = jObj("hits").num.toInt
      val scrollIdOpt = jObj
          .get("scroll_id")
          // This is necessary because it isn't real null but instead ujson.Null.
          .flatMap { stringOrNull => if (stringOrNull.isNull) None else Some(stringOrNull) }
          .map(_.str)
      val results = jObj("results").arr
      val size = results.size

      results.foreach { result =>
        val jObj = result.obj
        val dojoDocument = new IndicatorDocument(jObj)
        val datasetId = dojoDocument.id

        if (dojoDocument.skip)
          DojoRestScraper.logger.info(s"Skip scraping deprecated DOJO indicator with datasetId $datasetId")
        else {
          DojoRestScraper.logger.info(s"Scraping DOJO indicator with datasetId $datasetId")
          dojoDocument.outputs.foreach { dojoOutput =>
            writeDojoRecord(dojoDocument, dojoOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
          }
  //        dojoDocument.qualifierOutputsOpt.map { qualifierOutputs =>
  //          qualifierOutputs.foreach { qualifierOutput =>
  //            writeDojoRecord(dojoDocument, qualifierOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
  //          }
  //        }
        }
      }

      if (scrollIdOpt.isDefined && size >= indicatorsUrl.size) {
        val jVal = getJVal(indicatorsUrl.getTailUrl(scrollIdOpt.get))
        loop(jVal)
      }
    }

    try {
      val jVal = getJVal(indicatorsUrl.getHeadUrl())
      loop(jVal)
    }
    catch {
      case NonFatal(throwable) =>
        println(throwable.getMessage)
        throw throwable
    }
  }

  def scrape(tsvWriter: TsvWriter): Unit = {
    scrapeModels(tsvWriter)
    scrapeIndicators(tsvWriter)
  }
}

object DojoRestScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def fromConfig(config: Config): DojoRestScraper = {
    val baseUrl = config.getString("DojoRestScraper.url")
    val login = config.getString("DojoRestScraper.login")
    val propertiesBuilder = PropertiesBuilder.fromFile(login)
    val username = propertiesBuilder.getProperty("username").get
    val password = propertiesBuilder.getProperty("password").get

    new DojoRestScraper(baseUrl, username, password)
  }
}

class DatasetsUrl(baseUrl: String, path: String, query: String = DatasetsUrl.defaultQuery, val size: Int = DatasetsUrl.defaultSize) {
  val encodedQuery = encode(query)

  def encode(parameter: String): String = URLEncoder.encode(parameter, StandardCharsets.UTF_8.toString)

  def getHeadUrl(): String = {
    s"$baseUrl/$path?q=$encodedQuery&size=$size"
  }

  def getTailUrl(scrollId: String): String = {
    val encodedScrollId = scrollId // encode(scrollId)
    s"$baseUrl/$path?q=$encodedQuery&scroll_id=$encodedScrollId&size=$size"
  }
}

class IndicatorsUrl(baseUrl: String, query: String = DatasetsUrl.defaultQuery, size: Int = DatasetsUrl.defaultSize)
    extends DatasetsUrl(baseUrl, DatasetsUrl.defaultIndicatorsPath, query, size)

class ModelsUrl(baseUrl: String, query: String = DatasetsUrl.defaultQuery, size: Int = DatasetsUrl.defaultSize)
    extends DatasetsUrl(baseUrl, DatasetsUrl.defaultModelsPath, query, size)

object DatasetsUrl {
  val defaultQuery = "*"
  val defaultSize = 1000
  val defaultIndicatorsPath = "indicators"
  val defaultModelsPath = "models"
}
