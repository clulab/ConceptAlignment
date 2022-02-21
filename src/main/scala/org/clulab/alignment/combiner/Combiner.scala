package org.clulab.alignment.combiner

abstract class Combiner() {

  def immutableAddWeighted(left: Array[Float], right: Array[Float], weight: Float): Array[Float] =
      left.zip(right).map {
        case (left, right) => left + right * weight
      }

  def immutableAddWeightedOpt(left: Array[Float], rightOpt: Option[Array[Float]], weight: Float): Array[Float] =
      rightOpt.map(immutableAddWeighted(left, _, weight)).getOrElse(left)

  def mutableAddWeighted(left: Array[Float], right: Array[Float], weight: Float): Array[Float] = {
    var i = 0 // optimization

    while (i < left.length) {
      left(i) += (right(i) * weight)
      i += 1
    }
    left
  }

  def mutableAddWeightedOpt(left: Array[Float], rightOpt: Option[Array[Float]], weight: Float): Array[Float] =
    rightOpt.map(mutableAddWeighted(left, _, weight)).getOrElse(left)

  // All calls to this use a fresh array which can be mutated.
  def addWeightedOpt(left: Array[Float], rightOpt: Option[Array[Float]], weight: Float): Array[Float] =
      mutableAddWeightedOpt(left, rightOpt, weight)
}

object Combiner {
  val length = 300
}
