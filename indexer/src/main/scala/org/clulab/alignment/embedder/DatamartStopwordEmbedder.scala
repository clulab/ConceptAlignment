package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartStopwordEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val words = datamartEntry.words
    val filteredWords = words.filter(!DatamartStopwordEmbedder.stopwords(_))
    val embedding = w2v.makeCompositeVector(filteredWords)

    embedding
  }
}

object DatamartStopwordEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
