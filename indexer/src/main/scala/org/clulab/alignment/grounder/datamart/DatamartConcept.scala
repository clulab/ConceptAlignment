package org.clulab.alignment.grounder.datamart

import org.clulab.alignment.FlatConcept
import org.clulab.alignment.datamart.DatamartIdentifier

class DatamartConcept(val identifier: DatamartIdentifier, embedding: Array[Float])
    extends FlatConcept(identifier.toString, embedding) {
}

object DatamartConcept {
  def apply(identifier: DatamartIdentifier, embedding: Array[Float]) = new DatamartConcept(identifier, embedding)
}
