package org.clulab.alignment.webapp

import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue

class IndexerStatus(number: Int, text: String) extends Status {

  def toJsValue: JsValue = {
    JsObject(Seq(
      "number" -> JsNumber(number),
      "text" -> JsString(text)
    ))
  }
}

case object IndexerReady extends IndexerStatus(0, "ready")
case object IndexerBusy  extends IndexerStatus(1, "busy")
