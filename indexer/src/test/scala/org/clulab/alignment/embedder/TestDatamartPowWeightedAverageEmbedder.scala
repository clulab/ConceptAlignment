package org.clulab.alignment.embedder

import org.clulab.alignment.Test

class TestDatamartPowWeightedAverageEmbedder extends Test {

  behavior of "DatamartPowWeightedAverageEmbedder"

  it should "have nice weights" in {
    val weights = DatamartPowWeightedAverageEmbedder.weights

    println(weights.mkString("[", ", ", "]"))
  }
}
