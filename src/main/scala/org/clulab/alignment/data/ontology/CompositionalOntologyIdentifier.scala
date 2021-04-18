package org.clulab.alignment.data.ontology

import org.clulab.alignment.utils.Identifier
import play.api.libs.json.JsObject
import play.api.libs.json.Json

@SerialVersionUID(1L)
case class CompositionalOntologyIdentifier(
  conceptOntologyIdentifier: FlatOntologyIdentifier,
  conceptPropertyOntologyIdentifier: FlatOntologyIdentifier,
  processOntologyIdentifier: FlatOntologyIdentifier,
  processPropertyOntologyIdentifier: FlatOntologyIdentifier
) extends Identifier {

  override def toString(): String = s"$conceptOntologyIdentifier\t$conceptPropertyOntologyIdentifier\t$processOntologyIdentifier\t$processPropertyOntologyIdentifier"

  def toJsObject: JsObject = {
    Json.obj(
      "conceptId" -> conceptOntologyIdentifier.toString,
      "conceptPropertyId" -> conceptPropertyOntologyIdentifier.toString,
      "processId" -> processOntologyIdentifier.toString,
      "processPropertyId" -> processPropertyOntologyIdentifier.toString
    )
  }
}
