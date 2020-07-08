package org.clulab.alignment

import org.clulab.alignment

class EmbeddingOnlyAligner extends Aligner {

  val name = "EmbeddingOnly"

  override def align(c1: Concept, c2: Concept): Score = {
    // TODO: is this how we are supposed to do type transformation?
    val similarity = alignment.dotProduct(c1.embedding.asInstanceOf[Array[Float]], c2.embedding.asInstanceOf[Array[Float]])
    Score(name, similarity)
  }
}
