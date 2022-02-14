package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

abstract class DatamartEmbedder(w2v: CompactWordEmbeddingMap) {
  val unknownEmbedding = w2v.unknownEmbedding.toArray

  def embed(datamartEntry: DatamartEntry): Array[Float]

  // The words should have been lowercased previously.
  def filter(words: Array[String]): Array[String] = words.filterNot(DatamartEmbedder.stopwords)

  def makeCompositeVector(words: Array[String]): Array[Float] = {

    def nonEmpty(values: Array[Float]) = values.exists(_ != 0)

    // There could be a zero vector even with vocabulary.
    val compositeVector = w2v.makeCompositeVector(words)

    if (compositeVector.nonEmpty) compositeVector
    else unknownEmbedding
  }

  def scale(values: Array[Float], factor: Float): Array[Float] = values.map(_ * factor)
}

object DatamartEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
