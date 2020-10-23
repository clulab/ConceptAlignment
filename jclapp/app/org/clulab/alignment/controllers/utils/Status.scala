package org.clulab.alignment.controllers.utils

import org.slf4j.Logger
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue

class Status(number: Int, text: String) {

  def toJsValue: JsValue = {
    JsObject(Seq(
      "number" -> JsNumber(number),
      "text" -> JsString(text)
    ))
  }
}

case object Ready extends Status(0, "ready")
case object Busy  extends Status(1, "busy")

class StatusHolder(logger: Logger, protected var status: Status) {
  logger.info(s"Status is now $status")

  def get: Status = synchronized {
    status
  }

  def set(newStatus: Status): Unit = synchronized {
    status = newStatus
    logger.info(s"Status is now $status")
  }

  def toJsValue: JsValue = synchronized {
    status.toJsValue
  }
}
