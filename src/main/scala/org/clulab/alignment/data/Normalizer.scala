package org.clulab.alignment.data

class Normalizer {

  def normalize(array: Array[Float]): Option[Array[Float]] = normalize(array, length(array))

  def normalize(array: Array[Float], len: Float): Option[Array[Float]] = {
    if (len != 0) {
      array.indices.foreach { index => array(index) /= len }
      Some(array)
    }
    else
      None
  }

  def length(array: Array[Float]): Float = {
    val length2 = array.foldLeft(0f) { case (total: Float, item: Float) => total + item * item }
    val length = math.sqrt(length2).toFloat

    length
  }
}

object Normalizer {

  def apply(): Normalizer = new Normalizer()
}
