package org.clulab.normalizer

class MutableNormalizer() extends Normalizer() {

  def divide(array: Array[Float], divisor: Float): Array[Float] = {
    var i = 0 // optimization

    while (i < array.length) {
      array(i) /= divisor
      i += 1
    }
    array
  }

  def normalize(array: Array[Float], len: Float): Option[Array[Float]] =
      if (len != 0) Some(divide(array, len))
      else None

  def normalizeOrElse(array: Array[Float], default: => Array[Float]): Array[Float] = {
    normalize(array).getOrElse(default)
  }
}

object MutableNormalizer {
  lazy val instance = new MutableNormalizer()

  def apply(): MutableNormalizer = instance
}
