package org.clulab.alignment.groundings

import org.clulab.alignment.FlatConcept

class DatamartConcept(val identifier: DatamartIdentifier, embedding: Array[Float])
    extends FlatConcept(identifier.toString, embedding) {
}

object DatamartConcept {
  def apply(identifier: DatamartIdentifier, embedding: Array[Float]) = new DatamartConcept(identifier, embedding)
}
