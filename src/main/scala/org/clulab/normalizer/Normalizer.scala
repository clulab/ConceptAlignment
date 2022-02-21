package org.clulab.normalizer

abstract class Normalizer() {

  def normalize(array: Array[Float]): Option[Array[Float]] = normalize(array, length(array))

  def normalize(array: Array[Float], len: Float): Option[Array[Float]]

  // A zero vector will be returned if the array cannot be normalized.
  def normalizeOrElse(array: Array[Float]): Array[Float] = normalizeOrElse(array, array)

  def normalizeOrElse(array: Array[Float], default: => Array[Float]): Array[Float]

  def length(array: Array[Float]): Float = {
    val length2 = array.foldLeft(0f) { case (total: Float, item: Float) => total + item * item }
    val length = math.sqrt(length2).toFloat

    length
  }

  def isEmpty(array: Array[Float]): Boolean = array.forall(_ == 0f)

  def nonEmpty(array: Array[Float]): Boolean = !isEmpty(array)
}

