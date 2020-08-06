package org.clulab.alignment.data.ontology

import org.clulab.alignment.FlatConcept
import org.clulab.alignment.data.datamart.DatamartIdentifier

class OntologyConcept(val identifier: DatamartIdentifier, embedding: Array[Float])
    extends FlatConcept(identifier.toString, embedding) {
}

object DatamartConcept {
  def apply(identifier: DatamartIdentifier, embedding: Array[Float]) = new OntologyConcept(identifier, embedding)
}
