package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry

trait DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float]

  def filter(words: Array[String]): Array[String] = words.filter(DatamartEmbedder.stopwords)
}

object DatamartEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
