package org.clulab.alignment.combiner

abstract class Combiner() {
  // These operations work on immutable arrays.

  def mul(array: Array[Float], factor: Float): Array[Float] = array.map(_ * factor)

  def mulOpt(arrayOpt: Option[Array[Float]], factor: Float): Option[Array[Float]] =
      arrayOpt.map(mul(_, factor))

  def mul(left: Array[Float], right: Array[Float]): Array[Float] =
      left.zip(right).map {
        case (left, right) => left * right
      }

  def add(left: Array[Float], right: Array[Float], weight: Float): Array[Float] =
    left.zip(right).map {
      case (left, right) => left + right * weight
    }

  def add(left: Array[Float], right: Array[Float]): Array[Float] =
      left.zip(right).map {
        case (left, right) => left + right
      }

  def addOpt(left: Array[Float], rightOpt: Option[Array[Float]]): Array[Float] =
      rightOpt.map(add(left, _)).getOrElse(left)

  def addWeightedOpt(left: Array[Float], rightOpt: Option[Array[Float]], weight: Float): Array[Float] =
      rightOpt.map(add(left, _, weight)).getOrElse(left)
}

object Combiner {
  val length = 300
}
