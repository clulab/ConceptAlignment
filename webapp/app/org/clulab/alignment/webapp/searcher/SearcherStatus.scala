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

case object SearcherFailing  extends SearcherStatus(-1, "failing")
case object SearcherIdling   extends SearcherStatus( 0, "idling")
case object SearcherLoading  extends SearcherStatus( 1, "loading")
case object SearcherUpdating extends SearcherStatus( 1, "updating")
