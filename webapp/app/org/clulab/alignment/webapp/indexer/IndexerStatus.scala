package org.clulab.alignment.webapp.indexer

import org.clulab.alignment.webapp.utils.Status
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

object IndexerStatus {
  case object Crashing extends IndexerStatus(-2, "crashing")
  case object Failing  extends IndexerStatus(-1, "failing")
  case object Idling   extends IndexerStatus( 0, "idling")
  case object Loading  extends IndexerStatus( 1, "loading")
  case object Indexing extends IndexerStatus( 1, "indexing")
}
