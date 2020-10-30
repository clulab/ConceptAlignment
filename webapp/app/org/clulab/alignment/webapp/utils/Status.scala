package org.clulab.alignment.webapp.utils

import play.api.libs.json.JsValue

trait Status {
  def toJsValue: JsValue
}
