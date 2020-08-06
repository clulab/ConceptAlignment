package org.clulab.alignment.data.ontology

import org.clulab.alignment.utils.Identifier

class OntologyIdentifier(val ontologyName: String, val nodeName: String, val branchOpt: Option[String]) extends Identifier {

  override def toString(): String = s"$ontologyName//$nodeName"
}
