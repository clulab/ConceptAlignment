package org.clulab.alignment.webapp

import org.slf4j.Logger
import play.api.libs.json.JsValue

class StatusHolder[T <: Status](logger: Logger, protected var status: T) {
  logger.info(s"Status is now $status")

  def get: T = synchronized {
    status
  }

  def set(newStatus: T): Unit = synchronized {
    status = newStatus
    logger.info(s"Status is now $status")
  }

  def toJsValue: JsValue = synchronized {
    status.toJsValue
  }
}
