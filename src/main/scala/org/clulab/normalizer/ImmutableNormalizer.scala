package org.clulab.normalizer

class ImmutableNormalizer() extends Normalizer() {

  def divide(array: Array[Float], divisor: Float): Array[Float] =
      array.map(_ / divisor)

  def normalize(array: Array[Float], len: Float): Option[Array[Float]] =
      if (len != 0) Some(divide(array, len))
      else None

  def normalizeOrElse(array: Array[Float], default: => Array[Float]): Array[Float] =
      // Clone the default vector so that it could never ever be changed.
      normalize(array).getOrElse(default.clone())
}

object ImmutableNormalizer {
  lazy val instance = new ImmutableNormalizer()

  def apply(): ImmutableNormalizer = instance
}
