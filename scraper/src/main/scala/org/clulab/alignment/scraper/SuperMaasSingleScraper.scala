package org.clulab.alignment.scraper

import org.clulab.alignment.utils.TsvWriter
import ujson.Value

import scala.collection.mutable.{HashSet => MutableHashSet}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// The value for createSince has to match the format coming from SuperMaaS.
// In general, the value should be the greatest value seen to date so that
// each dataset delivered has a value greater that it.
abstract class SuperMaasSingleScraper(baseUrl: String, createdSince: String = "") extends DatamartScraper {

  def encode(parameter: String): String = URLEncoder.encode(parameter, StandardCharsets.UTF_8.toString)

  def stringElse(value: Value, key: String, consolation: String): String = {
    if (value.obj.contains(key))
      value(key).str
    else
      consolation
  }

  def arrElseEmpty(value: Value, key: String): IndexedSeq[Value] = {
    if (value.obj.contains(key) && ! value(key).isNull)
      value(key).arr.toIndexedSeq
    else
      IndexedSeq.empty[Value]
  }

  def tagsToJson(tags: IndexedSeq[String]): String = {
    val value = upickle.default.writeJs(tags)
    val json = ujson.write(value)

    json
  }

  case class VariableContext(
    datamartId: String,
    datasetId: String,
    datasetName: String,
    datasetTags: IndexedSeq[String],
    datasetDescription: String,
    datasetUrl: String
  )

  protected def scrapeLongVariables(tsvWriter: TsvWriter, variableContext: VariableContext, variables: IndexedSeq[Value]): Unit = {
    val variableIds: MutableHashSet[String] = MutableHashSet.empty

    variables.foreach { variable =>
      val parameterName = variable("name").str // Compulsory if exposed
      val parameterDescription = stringElse(variable, "description", "") // Optional
      val parameterTags = arrElseEmpty(variable, "tags").map(_.str)

      val variableId = parameterName // It doesn't have an ID otherwise.
      val variableName = parameterName
      val variableTags = parameterTags
      val variableDescription = parameterDescription

      if (variableIds.contains(variableId))
        SuperMaasScraper.logger.error(s"The SuperMaaS (datamartId, dataset_id, variable_id) of (${variableContext.datamartId}, ${variableContext.datasetId}, $variableId) is duplicated and skipped.")
      else {
        variableIds.add(variableId)
        tsvWriter.println(
          variableContext.datamartId,
          variableContext.datasetId,
          variableContext.datasetName,
          tagsToJson(variableContext.datasetTags),
          variableContext.datasetDescription,
          variableContext.datasetUrl,
          variableId,
          variableName,
          tagsToJson(variableTags),
          variableDescription
        )
      }
    }
  }

  protected def scrapeShortVariables(tsvWriter: TsvWriter, variableContext: VariableContext, variables: IndexedSeq[Value]): Unit = {
    val variableIds: MutableHashSet[String] = MutableHashSet.empty

    variables.foreach { variable =>
      val parameterName = variable("name").str
      val parameterDescription = stringElse(variable, "description", "")
      val parameterTags = arrElseEmpty(variable, "tags").map(_.str)

      val variableId = parameterName // It doesn't have an ID otherwise.
      val variableName = parameterName
      val variableTags = parameterTags
      val variableDescription = parameterDescription

      if (variableIds.contains(variableId))
        SuperMaasScraper.logger.error(s"The SuperMaaS (datamartId, dataset_id, variable_id) of (${variableContext.datamartId}, ${variableContext.datasetId}, $variableId) is duplicated and skipped.")
      else {
        variableIds.add(variableId)
        tsvWriter.println(
          variableContext.datamartId,
          variableContext.datasetId,
          variableContext.datasetName,
          tagsToJson(variableContext.datasetTags),
          variableContext.datasetDescription,
          variableContext.datasetUrl,
          variableId,
          variableName,
          tagsToJson(variableTags),
          variableDescription
        )
      }
    }
  }
}
