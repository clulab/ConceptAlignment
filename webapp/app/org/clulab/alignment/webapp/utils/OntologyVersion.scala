package org.clulab.alignment.webapp.utils

import org.clulab.alignment.utils.PropertiesBuilder

object OntologyVersion {

  def get(propertiesPath: String): String = {
    val properties = PropertiesBuilder.fromResource(propertiesPath).get
    val hash = Option(properties.getProperty("hash"))
    val ontologyVersion = hash.getOrElse("<unknown>")

    ontologyVersion
  }
}

object OntologyVersionApp extends App {
  val compVersion = OntologyVersion.get("/org/clulab/wm/eidos/english/ontologies/CompositionalOntology_metadata.properties")
  val flatVersion = OntologyVersion.get("/org/clulab/wm/eidos/english/ontologies/wm_flat_metadata.properties")

  println(s"compVersion = $compVersion")
  println(s"flatVersion = $flatVersion")
}