package org.clulab.alignment.aligner

import org.clulab.alignment.Concept
import org.clulab.alignment.utils.DotProduct

class EmbeddingOnlyAligner extends Aligner {

  val name = "EmbeddingOnly"

  override def align(c1: Concept, c2: Concept): ScoredPair = {
    val similarity = DotProduct.calculate(getEmbedding(c1), getEmbedding(c2))
    ScoredPair(name, c1, c2, similarity)
  }
}
