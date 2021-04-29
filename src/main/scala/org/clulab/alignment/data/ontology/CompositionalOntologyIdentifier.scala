package org.clulab.alignment.data.ontology

import org.clulab.alignment.utils.Identifier
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json

@SerialVersionUID(1L)
case class CompositionalOntologyIdentifier(
  conceptOntologyIdentifier: FlatOntologyIdentifier, conceptPropertyOntologyIdentifierOpt: Option[FlatOntologyIdentifier],
  processOntologyIdentifierOpt: Option[FlatOntologyIdentifier], processPropertyOntologyIdentifierOpt: Option[FlatOntologyIdentifier]
) extends Identifier {

  // This is the legacy version in which everything exists.
  def this(
    conceptOntologyIdentifier: FlatOntologyIdentifier, conceptPropertyOntologyIdentifier: FlatOntologyIdentifier,
    processOntologyIdentifier: FlatOntologyIdentifier, processPropertyOntologyIdentifier: FlatOntologyIdentifier
  ) = this(
    conceptOntologyIdentifier, Some(conceptPropertyOntologyIdentifier),
    Some(processOntologyIdentifier), Some(processPropertyOntologyIdentifier)
  )

  def this(
    conceptOntologyName: String, conceptPropertyOntologyNameOpt: Option[String],
    processOntologyNameOpt: Option[String], processPropertyOntologyNameOpt: Option[String]
  ) = this(
    FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptOntologyName, Some(CompositionalOntologyIdentifier.concept)),
    conceptPropertyOntologyNameOpt.map(name => FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, name, Some(CompositionalOntologyIdentifier.property))),
            processOntologyNameOpt.map(name => FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, name, Some(CompositionalOntologyIdentifier.process))),
    processPropertyOntologyNameOpt.map(name => FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, name, Some(CompositionalOntologyIdentifier.property)))
  )

  def this(
    conceptOntologyName: String, conceptPropertyOntologyName: String,
    processOntologyName: String, processPropertyOntologyName: String
  ) = this(
    conceptOntologyName, Some(conceptPropertyOntologyName),
    Some(processOntologyName), Some(processPropertyOntologyName)
  )

  override def toString(): String = s"$conceptOntologyIdentifier\t$conceptPropertyOntologyIdentifierOpt\t$processOntologyIdentifierOpt\t$processPropertyOntologyIdentifierOpt"

  def toJsObject: JsObject = {
    Json.obj(
      CompositionalOntologyIdentifier.concept         -> conceptOntologyIdentifier.nodeName,
      CompositionalOntologyIdentifier.conceptProperty -> conceptPropertyOntologyIdentifierOpt.map(_.nodeName),
      CompositionalOntologyIdentifier.process         ->         processOntologyIdentifierOpt.map(_.nodeName),
      CompositionalOntologyIdentifier.processProperty -> processPropertyOntologyIdentifierOpt.map(_.nodeName)
    )
  }
}

object CompositionalOntologyIdentifier {
  val ontology = "wm_compositional"
  // These are the names of the branches, indexes, and 4-tuples.
  val concept = "concept"
  val process = "process"
  val property = "property"
  // These are names only used in the 4-tuples.
  val conceptProperty = "conceptProperty"
  val processProperty = "processProperty"

  def fromJsValue(jsValue: JsValue): CompositionalOntologyIdentifier = {
    val jsObject = jsValue.asInstanceOf[JsObject]

    def getAsOptString(name: String): Option[String] = (jsObject \ name).asOpt[String]

    val         conceptOpt = getAsOptString(concept)
    val conceptPropertyOpt = getAsOptString(conceptProperty)
    val         processOpt = getAsOptString(process)
    val processPropertyOpt = getAsOptString(processProperty)

    def getIdentifierOpt(valueOpt: Option[String], branch: String): Option[FlatOntologyIdentifier] =
        valueOpt.map(value => FlatOntologyIdentifier(ontology, value, Some(branch)))

    val conceptIdentifier = FlatOntologyIdentifier(ontology, conceptOpt.get, Some(concept))
    val conceptPropertyIdentifierOpt = getIdentifierOpt(conceptPropertyOpt, property)
    val         processIdentifierOpt = getIdentifierOpt(        processOpt, process)
    val processPropertyIdentifierOpt = getIdentifierOpt(processPropertyOpt, property)

    CompositionalOntologyIdentifier(
      conceptIdentifier, conceptPropertyIdentifierOpt,
      processIdentifierOpt, processPropertyIdentifierOpt
    )
  }
}
