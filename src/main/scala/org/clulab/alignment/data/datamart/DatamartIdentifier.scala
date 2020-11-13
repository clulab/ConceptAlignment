package org.clulab.alignment.data.datamart

import org.clulab.alignment.utils.Identifier
import play.api.libs.json.JsObject
import play.api.libs.json.Json

@SerialVersionUID(1L)
case class DatamartIdentifier(datamartId: String, datasetId: String, variableId: String) extends Identifier {

  override def toString(): String = s"$datamartId/$datasetId/$variableId"

  def toJsObject: JsObject = {
    Json.obj(
      "datamartId" -> datamartId,
      "datasetId" -> datasetId,
      "variableId" -> variableId
    )
  }
}
