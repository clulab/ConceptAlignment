package org.clulab.alignment

class EmbeddingOnlyAligner extends Aligner {

  val name = "EmbeddingOnly"

  override def align(c1: Concept, c2: Concept): Score = {
    val similarity = dotProduct(c1.embedding, c2.embedding)
    Score(name, similarity)
  }
}
