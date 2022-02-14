package org.clulab.alignment.embedder

import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

object DatamartExpWeightedAverageEmbedder {
  val rawWeights: Array[Double] = Array(5.0, 1.0, 0.0, 17.0)
  val weights: Array[Float] = {
    val expWeights = rawWeights.map(math.exp)
    val sum = expWeights.sum

    expWeights.map { weight => (weight / sum).toFloat }
  }

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}
