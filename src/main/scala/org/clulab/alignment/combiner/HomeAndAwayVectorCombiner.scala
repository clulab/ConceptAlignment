package org.clulab.alignment.combiner

import org.clulab.normalizer.ImmutableNormalizer

class HomeAndAwayVectorCombiner(val homeWeight: Float, val awayWeight: Float) extends Combiner() {
  require(homeWeight > 0)
  require(awayWeight >= 0)

  def combine(homeVector: Array[Float], awayVectors: Array[Array[Float]]): Array[Float] = {
    if (awayVectors.nonEmpty) {
      val step0 = new Array[Float](Combiner.length)
      val step1 = addWeightedOpt(step0, Some(homeVector), homeWeight)
      val step2 = awayVectors.foldLeft(step1) { case (sum, array) => addWeightedOpt(sum, Some(array), awayWeight / awayVectors.length ) }
      val normalized = ImmutableNormalizer().normalizeOrElse(step2)

      normalized
    }
    else
      homeVector
  }
}
