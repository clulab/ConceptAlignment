package org.clulab.alignment.combiner

import org.clulab.normalizer.ImmutableNormalizer

class HomeAndAwayVectorCombiner(val homeWeight: Float, val awayWeight: Float) extends Combiner() {
  require(homeWeight >= 0)
  require(awayWeight >= 0)
  require(homeWeight + awayWeight > 0)

  def combine(homeVector: Array[Float], awayVectors: Array[Array[Float]]): Array[Float] = {
    if (awayVectors.nonEmpty) {
      val step0 = new Array[Float](Combiner.length)
      val step1 = addWeightedOpt(step0, Some(homeVector), homeWeight)
      val step2 = awayVectors.foldLeft(step1) { case (sum, array) => addWeightedOpt(sum, Some(array), awayWeight / awayVectors.length ) }
      val combined = step2
      // This returns a zero vector if the vector could not be normalized!
      val normalized = ImmutableNormalizer().normalizeOrElse(combined) // This can be mutable, because it is a new array.

      normalized
    }
    else
      homeVector
  }
}
