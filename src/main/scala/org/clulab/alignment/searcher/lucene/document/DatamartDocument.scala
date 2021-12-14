package org.clulab.alignment.searcher.lucene.document

import org.apache.lucene.document.Document
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.utils.SafeScore
import play.api.libs.json.JsObject
import play.api.libs.json.Json

class DatamartDocument(document: Document) {

  def datamartId = document.get("datamartId")

  def datasetId = document.get("datasetId")

  def variableId = document.get("variableId")

  def datamartIdentifier: DatamartIdentifier = DatamartIdentifier(datamartId, datasetId, variableId)

  def variableName: String = document.get("variableName")

  def variableDescription: String = document.get("variableDescription")

  def toJsObject(score: Float): JsObject = {
    Json.obj(
      "score" -> SafeScore.get(score),
      "datamartId" -> datamartId,
      "datasetId" -> datasetId,
      "variableId" -> variableId,
      "variableName" -> variableName,
      "variableDescription" -> variableDescription
    )
  }

  def toJsObject: JsObject = {
    Json.obj(
      "datamartId" -> datamartId,
      "datasetId" -> datasetId,
      "variableId" -> variableId,
      "variableName" -> variableName,
      "variableDescription" -> variableDescription
    )
  }
}
