package org.clulab.alignment

//TODO: this array import might not be correct.

case class Concept(name: String, embedding: Array[Float])

case class Score(name: String, value: Float)

trait Aligner {
  val name: String
  def align(c1: Concept, c2: Concept): Score
}
