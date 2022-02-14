package org.clulab.alignment.embedder

import org.clulab.embeddings.CompactWordEmbeddingMap

object DatamartEpsWeightedAverageEmbedder {
  val epsilon = 0.1
  val rawWeights: Array[Double] = Array(5.0, 1.0, 0.0, 17.0)
  val weights: Array[Float] = {
    val epsWeights = rawWeights.map(_ + epsilon)
    val sum = epsWeights.sum

    epsWeights.map { weight => (weight / sum).toFloat }
  }

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}