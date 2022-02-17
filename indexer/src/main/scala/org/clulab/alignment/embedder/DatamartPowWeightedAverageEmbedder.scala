package org.clulab.alignment.embedder

import org.clulab.embeddings.CompactWordEmbeddingMap

object DatamartPowWeightedAverageEmbedder {
  val rawWeights: Array[Double] = Array(5.0, 1.0, 0.0, 17.0)
  // pow = 2.0 => [0.24962536, 0.24774495, 0.24727707, 0.25535265], 13 correct
  // pow = 1.0 => [0.23095006, 0.1940835, 0.1858259, 0.38914055], 17 correct
  val weights: Array[Float] = {
    val sumSqr = math.pow(rawWeights.sum, 1.0)

    DatamartWeightedAverageEmbedder.softmax(rawWeights.map(_ / sumSqr))
  }

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}
