package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartSingleEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val datasetTags = datamartEntry.datasetTags
    val datasetDescription = datamartEntry.datasetDescription
    val variableTags = datamartEntry.variableTags
    val variableDescription = datamartEntry.variableDescription

    val filteredDatasetTags = datasetTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredDatasetDescription = datasetDescription.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableTags = variableTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableDescription = variableDescription.filter(!DatamartStopwordEmbedder.stopwords(_))

    val embedding = w2v.makeCompositeVector(filteredVariableTags)

    embedding
  }
}

object DatamartSingleEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
