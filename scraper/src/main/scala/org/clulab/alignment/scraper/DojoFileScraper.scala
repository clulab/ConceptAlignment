package org.clulab.alignment.scraper

import com.typesafe.config.Config
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.utils.{Sourcer, TsvWriter}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.slf4j.{Logger, LoggerFactory}

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

abstract class DojoDocument(json: String) {
  protected val jObj: mutable.Map[String, ujson.Value] = ujson.read(json).obj
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

class ModelDocument(json: String) extends DojoDocument(json) {
  val parameters: Array[DojoParameter] = DojoDocument.getParameters(jObj, this) // required
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

class IndicatorDocument(json: String) extends DojoDocument(json) {
  val outputs: Array[DojoOutput] = DojoDocument.getOutputs(jObj, this) // required
  val qualifierOutputsOpt: Option[Array[DojoQualifierOutput]] = DojoDocument.getQualifierOutputsOpt(jObj, this)
}

class DojoFileScraper(filename: String) extends DatamartScraper {

  def scrapeModel(): Unit = {
  }

  def writeDojoRecord(dojoDocument: DojoDocument, dojoVariable: DojoVariable, tsvWriter: TsvWriter, doubleIds: MutableHashSet[(String, String)]): Unit = {
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
      DojoFileScraper.logger.error(s"The DOJO (dataset_id, variable_id) of ($datasetId, $variableId) is duplicated and skipped.")
    else {
      tsvWriter.println(
        DojoFileScraper.datamartId,
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

  def scrape(tsvWriter: TsvWriter): Unit = {
    val doubleIds: MutableHashSet[(String, String)] = MutableHashSet.empty

    Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.foreach { line =>
        val dojoDocument = new IndicatorDocument(line)
        val datasetId = dojoDocument.id

        DojoFileScraper.logger.info(s"Scraping DOJO datasetId $datasetId")
        dojoDocument.outputs.foreach { dojoOutput =>
          writeDojoRecord(dojoDocument, dojoOutput, tsvWriter, doubleIds)
        }
//        dojoDocument.qualifierOutputsOpt.foreach { dojoQualifierOutputs =>
//          dojoQualifierOutputs.foreach { dojoQualifierOutput =>
//            writeDojoRecord(dojoDocument, dojoQualifierOutput, tsvWriter, doubleIds)
//          }
//        }
      }
    }
  }
}

object DojoFileScraper {
  protected lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val datamartId = "DOJO"

  def fromConfig(config: Config): DojoFileScraper = {
    val filename = config.getString("DojoFileScraper.filename")

    new DojoFileScraper(filename)
  }
}
