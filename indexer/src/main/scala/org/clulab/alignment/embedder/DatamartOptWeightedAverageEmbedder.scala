package org.clulab.alignment.embedder

import org.clulab.embeddings.CompactWordEmbeddingMap

object DatamartOptWeightedAverageEmbedder {
  // These "optimized" values were arrived at through experimentation.
  val weights: Array[Float] = Array(0.1f, 0.3f, 0.1f, 0.5f)

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}
