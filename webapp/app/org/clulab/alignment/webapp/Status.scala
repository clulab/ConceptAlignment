package org.clulab.alignment.webapp

import play.api.libs.json.JsValue

trait Status {
  def toJsValue: JsValue
}
