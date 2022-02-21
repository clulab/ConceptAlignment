package org.clulab.alignment.combiner

import org.clulab.normalizer.{ImmutableNormalizer, Normalizer}

class ContextAndOntologyCombiner(contextWeight: Float, ontologyWeight: Float) extends Combiner() {
  require(contextWeight >= 0)
  require(ontologyWeight >= 0)
  require(contextWeight + ontologyWeight > 0)

  def combine(contextVectorOpt: Option[Array[Float]], ontologyVectorOpt: Option[Array[Float]]): Array[Float] = {
    val step0 = new Array[Float](Combiner.length)
    val step1 = addWeightedOpt(step0, contextVectorOpt, contextWeight)
    val step2 = addWeightedOpt(step1, ontologyVectorOpt, ontologyWeight)
    val combined = step2
    // This returns a zero vector if the vector could not be normalized!
    val normalized = ImmutableNormalizer().normalizeOrElse(combined) // This can be mutable, because it is a new array.

    normalized
  }
}
