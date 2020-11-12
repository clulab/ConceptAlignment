package org.clulab.alignment.utils

object SafeScore {

  // This could return an Option[Float] instead.  Alternatively, the values
  // with NaN could be filtered out from the answers.
  def get(score: Float): Float = if (score.isNaN) 0f else score
}
