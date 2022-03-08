package org.clulab.alignment.scraper

import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger

import scala.collection.mutable
import scala.collection.mutable.{HashSet => MutableHashSet}
import scala.util.Try

class DojoVariable(jVal: ujson.Value, dojoDocument: DojoDocument) {
  protected val jObj: mutable.Map[String, ujson.Value] = jVal.obj
  val name: String = jObj("name").str // required
  val displayName: String = jObj("display_name").str // required
  val description: String = jObj("description").str // required
  val words: Array[String] = {
    val tokenizer = Tokenizer()
    val displayNameWords = tokenizer.tokenize(displayName)
    val descriptionWords = tokenizer.tokenize(description)

    displayNameWords ++ descriptionWords ++ dojoDocument.getWords
  }
}

class DojoParameter(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoQualifierOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

abstract class DojoDocument(val jObj: mutable.Map[String, ujson.Value], val datamartId: String) {
  val skip: Boolean
  val id: String = jObj("id").str // required
  val name: String = jObj("name").str
  val description: String = jObj("description").str // required
  val categories: Array[String] = DojoDocument.getCategories(jObj) // required
  val tags: Array[String] = DojoDocument.getTags(jObj) // optional
  val geography: Array[String] = DojoDocument.getGeography(jObj) // optional
  val (periodGteOpt, periodLteOpt) = DojoDocument.getPeriod(jObj) // optional
  val words: Array[String] = {
    val tokenizer = Tokenizer()
    val descriptionWords = tokenizer.tokenize(description)
    val categoryWords = categories.flatMap(tokenizer.tokenize)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    descriptionWords ++ categoryWords ++ tagWords
  }

  def getWords: Array[String] = words
}

object DojoDocument {
  def asOption(jValue: ujson.Value): Option[ujson.Value] =
    Option(if (jValue.isNull) null else jValue)

  def getTags(jObj: mutable.Map[String, ujson.Value]): Array[String] = jObj
    .get("tags") // This is an option.
    .flatMap { tagsOrNull =>
      asOption(tagsOrNull).map { tags =>
        tags.arr.toArray.map(_.str)
      }
    }
    .getOrElse(Array.empty) // optional

  def getGeography(jObj: mutable.Map[String, ujson.Value]): Array[String] = {
    jObj.get("geography").map { geography =>

      def getStringArray(key: String): Array[String] = geography.obj.get(key)
          .flatMap(toOption) // It can be JNull.
          .map(_.arr.toArray.map(_.str))
          .getOrElse(Array.empty)

      val countries = getStringArray("country")
      val admin1 = getStringArray("admin1")
      val admin2 = getStringArray("admin2")
      val admin3 = getStringArray("admin3")

      countries ++ admin1 ++ admin2 ++ admin3
    }.getOrElse(Array.empty)
  }

  def toOption(jValue: ujson.Value): Option[ujson.Value] =
      if (jValue.isNull) None else Option(jValue)

  def getPeriod(jObj: mutable.Map[String, ujson.Value]): (Option[Long], Option[Long]) = jObj.get("period")
      .flatMap(toOption) // It can be JNull.
      .flatMap { period =>

        def getLongOpt(key: String): Option[Long] = period.obj.get(key)
            .flatMap(toOption) // It can be JNull.
            .flatMap { jValue =>
              Try(jValue.num.toLong).toOption.flatMap { long =>
                // If 0, assume that it doesn't really exist.
                if (long == 0) None
                else Some(long)
              }
            }

        val gte = getLongOpt("gte")
        val lte = getLongOpt("lte")

        Some(gte, lte)
      }.getOrElse((None, None))

  def getCategories(jObj: mutable.Map[String, ujson.Value]): Array[String] = asOption(jObj("category"))
    .map { categories =>
      categories.arr.toArray.map(_.str)
    }
    .getOrElse(Array.empty) // required, but perhaps the value is null

  def getParameters(jObj: mutable.Map[String, ujson.Value], dojoDocument: DojoDocument): Array[DojoParameter] = asOption(jObj("parameters"))
    .map { parameters =>
      parameters.arr.toArray.map(new DojoParameter(_, dojoDocument))
    }
    .getOrElse(Array.empty) // required, but perhaps the value is null

  def getOutputs(jObj: mutable.Map[String, ujson.Value], dojoDocument: DojoDocument): Array[DojoOutput] = asOption(jObj("outputs"))
    .map { outputs =>
      outputs.arr.toArray.map(new DojoOutput(_, dojoDocument))
    }
    .getOrElse(Array.empty) // required, but perhaps the value is null

  // This is different in that an Option is expected rather than an empty array when not specified.
  def getQualifierOutputsOpt(jObj: mutable.Map[String, ujson.Value], dojoDocument: DojoDocument): Option[Array[DojoQualifierOutput]] = jObj
    .get("qualifier_outputs")
    .map { qualifierOutputsOrNull =>
      asOption(qualifierOutputsOrNull).map { qualifierOutputs =>
        qualifierOutputs.arr.toArray.map(new DojoQualifierOutput(_, dojoDocument))
      }.getOrElse(Array.empty) // null will turn into Some(Array.empty)
    } // optional and Optional

  def stringsToJson(strings: IndexedSeq[String]): String = {
    val value = upickle.default.writeJs(strings)
    val json = ujson.write(value)

    json
  }
}

class ModelDocument(jObj: mutable.Map[String, ujson.Value]) extends DojoDocument(jObj, DojoScraper.datamartModelId) {
  val skip: Boolean = {
    val isPublished: Boolean = jObj.get("is_published").map(_.bool).getOrElse(false)
    val hasNextVersion: Boolean = jObj.get("next_version")
        .flatMap(DojoDocument.toOption) // It can be JNull.
        .map(_.str.nonEmpty)
        .getOrElse(false)
    hasNextVersion || !isPublished
  }
  val parameters: Array[DojoParameter] = DojoDocument.getParameters(jObj, this) // required
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

class IndicatorDocument(jObj: mutable.Map[String, ujson.Value]) extends DojoDocument(jObj, DojoScraper.datamartIndicatorId) {
  val skip: Boolean = {
    val deprecated: Boolean = jObj.get("deprecated").map(_.bool).getOrElse(false) // optional
    deprecated
  }
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

abstract class DojoScraper extends DatamartScraper {

  def writeDojoRecord(dojoDocument: DojoDocument, dojoVariable: DojoVariable, tsvWriter: TsvWriter, doubleIds: MutableHashSet[(String, String)], logger: Logger): Unit = {
    val datamartId = dojoDocument.datamartId
    val datasetId = dojoDocument.id
    val datasetName = dojoDocument.name
    val datasetTags = dojoDocument.tags
    val datasetDescription = dojoDocument.description
    val datasetUrl = ""
    val datasetGeography = dojoDocument.geography
    val datasetPeriodGte = dojoDocument.periodGteOpt.map(_.toString).getOrElse("")
    val datasetPeriodLte = dojoDocument.periodLteOpt.map(_.toString).getOrElse("")

    val variableId = dojoVariable.name
    val variableName = dojoVariable.displayName
    val variableDescription = dojoVariable.description
    val variableTags = Array.empty[String]

    val doubleId = (datasetId, variableId)
    if (doubleIds.contains(doubleId))
      logger.error(s"The DOJO (dataset_id, variable_id) of ($datasetId, $variableId) is duplicated and skipped.")
    else {
      tsvWriter.println(
        datamartId,
        datasetId,
        datasetName,
        DojoDocument.stringsToJson(datasetTags),
        datasetDescription,
        datasetUrl,

        DojoDocument.stringsToJson(datasetGeography),
        datasetPeriodGte,
        datasetPeriodLte,

        variableId,
        variableName,
        DojoDocument.stringsToJson(variableTags),
        variableDescription
      )
    }
  }
}

object DojoScraper {
  val datamartModelId = "DOJO_Model"
  val datamartIndicatorId = "DOJO_Indicator"
}
