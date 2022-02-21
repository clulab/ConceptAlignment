package org.clulab.alignment.combiner

import org.clulab.normalizer.{ImmutableNormalizer, Normalizer}

class ContextAndOntologyCombiner(contextWeight: Float, ontologyWeight: Float) extends Combiner() {
  require(contextWeight >= 0)
  require(ontologyWeight >= 0)
  require(contextWeight > 0 || ontologyWeight > 0)

  def combine(contextVectorOpt: Option[Array[Float]], ontologyVectorOpt: Option[Array[Float]]): Array[Float] = {
    val weightSum0 = 0f
    val weightSum1 = weightSum0 +  contextVectorOpt.map(_ =>  contextWeight).getOrElse(0f)
    val weightSum2 = weightSum1 + ontologyVectorOpt.map(_ => ontologyWeight).getOrElse(0f)
    val weightSum  = weightSum2

    val step0 = new Array[Float](Combiner.length)
    val step1 = addWeightedOpt(step0, contextVectorOpt, contextWeight / weightSum)
    val step2 = addWeightedOpt(step1, ontologyVectorOpt, ontologyWeight / weightSum)
    val combined = step2
    // This returns a zero vector if the vector could not be normalized!
    val normalized = ImmutableNormalizer().normalizeOrElse(combined)

    normalized
  }
}
