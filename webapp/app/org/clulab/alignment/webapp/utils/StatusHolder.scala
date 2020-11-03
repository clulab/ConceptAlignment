package org.clulab.alignment.webapp.utils

import org.slf4j.Logger
import play.api.libs.json.JsValue

class StatusHolder[T <: Status](name: String, logger: Logger, protected var status: T) {
  logger.info(s"$name status is now $status")

  def get: T = this.synchronized {
    status
  }

  def set(newStatus: T): Unit = this.synchronized {
    status = newStatus
    logger.info(s"$name status is now $newStatus")
  }

  def toJsValue: JsValue = this.synchronized {
    status.toJsValue
  }
}
