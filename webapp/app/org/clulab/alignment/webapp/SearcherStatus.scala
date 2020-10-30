package org.clulab.alignment.webapp

import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue

class SearcherStatus(number: Int, text: String) extends Status {

  def toJsValue: JsValue = {
    JsObject(Seq(
      "number" -> JsNumber(number),
      "text" -> JsString(text)
    ))
  }
}

case object SearcherReady extends SearcherStatus(0, "ready")
case object SearcherBusy  extends SearcherStatus(1, "busy")
