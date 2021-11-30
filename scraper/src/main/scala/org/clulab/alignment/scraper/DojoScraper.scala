package org.clulab.alignment.scraper

import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.utils.TsvWriter
import org.slf4j.Logger

import scala.collection.mutable
import scala.collection.mutable.{HashSet => MutableHashSet}

class DojoVariable(jVal: ujson.Value, dojoDocument: DojoDocument) {
  protected val jObj: mutable.Map[String, ujson.Value] = jVal.obj
  val name: String = jObj("name").str // required
  val displayName: String = jObj("display_name").str // required
  val description: String = jObj("description").str // required
  val tags: Array[String] = DojoDocument.getTags(jObj) // optional
  val words: Array[String] = {
    val tokenizer = Tokenizer()
    val displayNameWords = tokenizer.tokenize(displayName)
    val descriptionWords = tokenizer.tokenize(description)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    displayNameWords ++ descriptionWords ++ tagWords ++ dojoDocument.getWords
  }
}

class DojoParameter(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoQualifierOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

abstract class DojoDocument(val jObj: mutable.Map[String, ujson.Value]) {
  val id: String = jObj("id").str // required
  val name: String = jObj("name").str
  val description: String = jObj("description").str // required
  val categories: Array[String] = DojoDocument.getCategories(jObj) // required
  val tags: Array[String] = DojoDocument.getTags(jObj) // optional
  //  geography // optional // These won't be in our ontologies anyway.
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

  def tagsToJson(tags: IndexedSeq[String]): String = {
    val value = upickle.default.writeJs(tags)
    val json = ujson.write(value)

    json
  }
}

class ModelDocument(jObj: mutable.Map[String, ujson.Value]) extends DojoDocument(jObj) {
  val parameters: Array[DojoParameter] = DojoDocument.getParameters(jObj, this) // required
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

class IndicatorDocument(jObj: mutable.Map[String, ujson.Value]) extends DojoDocument(jObj) {
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

abstract class DojoScraper extends DatamartScraper {

  def writeDojoRecord(dojoDocument: DojoDocument, dojoVariable: DojoVariable, tsvWriter: TsvWriter, doubleIds: MutableHashSet[(String, String)], logger: Logger): Unit = {
    val datasetId = dojoDocument.id
    val datasetName = dojoDocument.name
    val datasetTags = dojoDocument.tags
    val datasetDescription = dojoDocument.description
    val datasetUrl = ""

    val variableId = dojoVariable.name
    val variableName = dojoVariable.displayName
    val variableDescription = dojoVariable.description
    val variableTags = dojoVariable.tags

    val doubleId = (datasetId, variableId)
    if (doubleIds.contains(doubleId))
      logger.error(s"The DOJO (dataset_id, variable_id) of ($datasetId, $variableId) is duplicated and skipped.")
    else {
      tsvWriter.println(
        DojoScraper.datamartId,
        datasetId,
        datasetName,
        DojoDocument.tagsToJson(datasetTags),
        datasetDescription,
        datasetUrl,
        variableId,
        variableName,
        DojoDocument.tagsToJson(variableTags),
        variableDescription
      )
    }
  }
}

object DojoScraper {
  val datamartId = "DOJO"
}
