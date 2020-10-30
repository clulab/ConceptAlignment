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

case object IndexerFailing  extends IndexerStatus(-1, "failing")
case object IndexerIdling   extends IndexerStatus( 0, "idling")
case object IndexerLoading  extends IndexerStatus( 1, "loading")
case object IndexerIndexing extends IndexerStatus( 1, "indexing")
