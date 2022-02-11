package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartAverageEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val datasetTags = datamartEntry.datasetTags
    val datasetDescription = datamartEntry.datasetDescription
    val variableTags = datamartEntry.variableTags
    val variableDescription = datamartEntry.variableDescription

    val filteredDatasetTags = datasetTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredDatasetDescription = datasetDescription.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableTags = variableTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableDescription = variableDescription.filter(!DatamartStopwordEmbedder.stopwords(_))

    val embedding1 = w2v.makeCompositeVector(filteredDatasetTags)
    val embedding2 = w2v.makeCompositeVector(filteredDatasetDescription)
    val embedding3 = w2v.makeCompositeVector(filteredVariableTags)
    val embedding4 = w2v.makeCompositeVector(filteredVariableDescription)

    val embedding = Array(embedding1, embedding2, embedding3, embedding4).transpose.map(_.sum).map(x => x/4)

    embedding
  }
}

object DatamartAverageEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
