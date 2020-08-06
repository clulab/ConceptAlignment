package org.clulab.alignment.data.ontology

import org.clulab.alignment.utils.Identifier

@SerialVersionUID(1L)
case class OntologyIdentifier(ontologyName: String, nodeName: String, branchOpt: Option[String]) extends Identifier {

  override def toString(): String = s"$ontologyName//$nodeName"
}
