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
  protected val readTimeout = 60000
  val datasetsUrl = new DatasetsUrl(baseUrl)

  def getJVal(datasetsUrl: String): ujson.Value = {
    val json = requests.get(datasetsUrl, auth = auth, readTimeout = readTimeout).text(StandardCharsets.UTF_8)
    ujson.read(json)
  }

  def scrapeModels(tsvWriter: TsvWriter): Unit = {
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

        DojoRestScraper.logger.info(s"Scraping DOJO datasetId $datasetId")
        dojoDocument.outputs.foreach { dojoOutput =>
          writeDojoRecord(dojoDocument, dojoOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
        }
//        dojoDocument.qualifierOutputsOpt.map { qualifierOutputs =>
//          qualifierOutputs.foreach { qualifierOutput =>
//            writeDojoRecord(dojoDocument, qualifierOutput, tsvWriter, doubleIds, DojoRestScraper.logger)
//          }
//        }
      }

      if (scrollIdOpt.isDefined && size >= DatasetsUrl.defaultSize) {
        val jVal = getJVal(datasetsUrl.getTailUrl(scrollIdOpt.get))
        loop(jVal)
      }
    }

    try {
      val jVal = getJVal(datasetsUrl.getHeadUrl())
      loop(jVal)
    }
    catch {
      case NonFatal(throwable) => println(throwable.getMessage)
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

class DatasetsUrl(baseUrl: String, query: String = DatasetsUrl.defaultQuery, size: Int = DatasetsUrl.defaultSize) {
  val encodedQuery = encode(query)

  def encode(parameter: String): String = URLEncoder.encode(parameter, StandardCharsets.UTF_8.toString)

  def getHeadUrl(): String = {
    s"$baseUrl/${DatasetsUrl.defaultRest}?q=$encodedQuery&size=$size"
  }

  def getTailUrl(scrollId: String): String = {
    val encodedScrollId = scrollId // encode(scrollId)
    s"$baseUrl/${DatasetsUrl.defaultRest}?q=$encodedQuery&scroll_id=$encodedScrollId&size=$size"
  }
}

object DatasetsUrl {
  val defaultQuery = "a"
  val defaultSize = 1000
  val defaultRest = "indicators"
}
