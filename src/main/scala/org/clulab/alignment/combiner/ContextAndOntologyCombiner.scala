package org.clulab.alignment.combiner

import org.clulab.alignment.data.Normalizer

class ContextAndOntologyCombiner(contextWeight: Float, ontologyWeight: Float) {
  val normalizer = Normalizer()

  def combine(contextVector: Array[Float], ontologyVector: Array[Float]): Array[Float] = {
    val weightedContext = contextVector.map(_ * contextWeight)
    val weightedOntology = ontologyVector.map(_ * ontologyWeight)
    val combined = Array(weightedContext, weightedOntology).transpose.map(_.sum)
    // This throws an exception if the vector could not be normalized!
    val normalized = normalizer.normalize(combined).get

    normalized
  }
}
