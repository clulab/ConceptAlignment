package org.clulab.alignment.webapp.searcher

import org.clulab.alignment.webapp.utils.Status
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

object SearcherStatus {
  case object Failing  extends SearcherStatus(-1, "failing")
  case object Idling   extends SearcherStatus( 0, "idling")
  case object Loading  extends SearcherStatus( 1, "loading")
}
