package org.clulab.alignment.combiner

import org.clulab.normalizer.MutableNormalizer

class CompositionalCombiner(val conceptWeight: Float, val conceptPropertyWeight: Float, val processWeight: Float,
    val processPropertyWeight: Float) extends Combiner() {
  require(conceptWeight >= 0)
  require(conceptPropertyWeight >= 0)
  require(processWeight >= 0)
  require(processPropertyWeight >= 0)
  require(conceptWeight + conceptPropertyWeight + processWeight + processPropertyWeight > 0)

  def combine(conceptVectorOpt: Option[Array[Float]], conceptPropertyVectorOpt: Option[Array[Float]],
              processVectorOpt: Option[Array[Float]], processPropertyVectorOpt: Option[Array[Float]]): Array[Float] = {
    val step0 = new Array[Float](Combiner.length)
    val step1 = addWeightedOpt(step0,         conceptVectorOpt,         conceptWeight)
    val step2 = addWeightedOpt(step1, conceptPropertyVectorOpt, conceptPropertyWeight)
    val step3 = addWeightedOpt(step2,         processVectorOpt,         processWeight)
    val step4 = addWeightedOpt(step3, processPropertyVectorOpt, processPropertyWeight)
    val combined = step4
    // This returns a zero vector if the vector could not be normalized!
    val normalized = MutableNormalizer().normalizeOrElse(combined) // This can be mutable, because it is a new array.

    normalized
  }
}
