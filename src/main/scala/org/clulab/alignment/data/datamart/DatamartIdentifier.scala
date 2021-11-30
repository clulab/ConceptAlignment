package org.clulab.alignment.data.datamart

import org.clulab.alignment.utils.Identifier
import play.api.libs.json.JsObject
import play.api.libs.json.Json

@SerialVersionUID(1L)
case class DatamartIdentifier(datamartId: String, datasetId: String, variableId: String) extends Identifier {

  def escape(string: String): String = string
      .replace("\\", "\\\\")
      .replace("/", "\\/")

  override def toString(): String =
      Seq(datamartId, datasetId, variableId)
          .map(escape)
          .mkString("/")

  def toJsObject: JsObject = {
    Json.obj(
      "datamartId" -> datamartId,
      "datasetId" -> datasetId,
      "variableId" -> variableId
    )
  }
}
