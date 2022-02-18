package org.clulab.alignment.data

class Normalizer() {

  def normalize(array: Array[Float]): Option[Array[Float]] = normalize(array, length(array))

  def normalize(array: Array[Float], len: Float): Option[Array[Float]] =
      if (len != 0) Some(array.map(_ / len))
      else None

  def normalizeOrElse(array: Array[Float], default: => Array[Float]): Array[Float] =
      normalize(array).getOrElse(default.clone())

  def length(array: Array[Float]): Float = {
    val length2 = array.foldLeft(0f) { case (total: Float, item: Float) => total + item * item }
    val length = math.sqrt(length2).toFloat

    length
  }
}

object Normalizer {
  protected val instance = new Normalizer()

  def apply(): Normalizer = instance
}
