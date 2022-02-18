package org.clulab.alignment.embedder

import org.clulab.embeddings.CompactWordEmbeddingMap

object DatamartPowWeightedAverageEmbedder {
  val rawWeights: Array[Double] = DatamartWeightedAverageEmbedder.defaultWeights
  // pow = 2.0 => [0.24962536, 0.24774495, 0.24727707, 0.25535265], 13 correct
  // pow = 1.0 => [0.23095006, 0.1940835, 0.1858259, 0.38914055], 17 correct
  // pow = 0.5 => [0.071449876, 0.031029506, 0.025189422, 0.8723312], X correct
  val weights: Array[Float] = {
    val sumPow = math.pow(rawWeights.sum, 0.5)

    DatamartWeightedAverageEmbedder.softmax(rawWeights.map(_ / sumPow))
  }

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}
