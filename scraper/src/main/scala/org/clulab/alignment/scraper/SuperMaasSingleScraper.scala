package org.clulab.alignment.scraper

import org.clulab.alignment.utils.TsvWriter
import ujson.Value

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// The value for createSince has to match the format coming from SuperMaaS.
// In general, the value should be the greatest value seen to date so that
// each dataset delivered has a value greater that it.
abstract class SuperMaasSingleScraper(baseUrl: String, createdSince: String = "") extends DatamartScraper {

  def encode(parameter: String): String = URLEncoder.encode(createdSince, StandardCharsets.UTF_8.toString)

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

  case class VariableContext(datamartId: String, datasetId: String, datasetName: String, datasetDescription: String, datasetUrl: String)

  protected def scrapeLongVariables(tsvWriter: TsvWriter, variableContext: VariableContext, variables: IndexedSeq[Value]): Unit = {
    variables.foreach { variable =>
      val parameterName = variable("name").str // Compulsory if exposed
      val parameterDescription = stringElse(variable, "description", "") // Optional

      val variableId = parameterName
      val variableName = parameterName
      val variableDescription = parameterDescription

      tsvWriter.println(variableContext.datamartId, variableContext.datasetId, variableContext.datasetName,
          variableContext.datasetDescription, variableContext.datasetUrl, variableId, variableName, variableDescription)
    }
  }

  protected def scrapeShortVariables(tsvWriter: TsvWriter, variableContext: VariableContext, variables: IndexedSeq[Value]): Unit = {
    variables.foreach { variable =>
      val parameterName = variable.str
      val parameterDescription = ""

      val variableId = parameterName
      val variableName = parameterName
      val variableDescription = parameterDescription

      tsvWriter.println(SuperMaasModelScraper.datamartId, variableContext.datasetId, variableContext.datasetName,
        variableContext.datasetDescription, variableContext.datasetUrl, variableId, variableName, variableDescription)
    }
  }
}
