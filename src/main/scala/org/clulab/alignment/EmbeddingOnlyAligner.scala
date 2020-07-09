package org.clulab.alignment

class EmbeddingOnlyAligner extends Aligner {

  val name = "EmbeddingOnly"

  override def align(c1: Concept, c2: Concept): ScoredPair = {
    val similarity = dotProduct(getEmbedding(c1), getEmbedding(c2))
    ScoredPair(name, c1, c2, similarity)
  }
}
