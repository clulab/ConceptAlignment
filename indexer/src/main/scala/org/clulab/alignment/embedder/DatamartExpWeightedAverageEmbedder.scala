package org.clulab.alignment.embedder

import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

object DatamartExpWeightedAverageEmbedder {
  val rawWeights: Array[Double] = DatamartWeightedAverageEmbedder.defaultWeights
  val weights: Array[Float] = DatamartWeightedAverageEmbedder.softmax(rawWeights)

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAverageEmbedder = new DatamartWeightedAverageEmbedder(w2v, weights)
}
