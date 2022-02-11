package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

class DatamartWeightedAverageEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val datasetTags = datamartEntry.datasetTags
    val datasetDescription = datamartEntry.datasetDescription
    val variableTags = datamartEntry.variableTags
    val variableDescription = datamartEntry.variableDescription

    val filteredDatasetTags = datasetTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredDatasetDescription = datasetDescription.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableTags = variableTags.filter(!DatamartStopwordEmbedder.stopwords(_))
    val filteredVariableDescription = variableDescription.filter(!DatamartStopwordEmbedder.stopwords(_))

    val w1 = 6.14417366e-06.toFloat
    val w2 = 1.12534466e-07.toFloat
    val w3 = 4.13991165e-08.toFloat
    val w4 = 9.99993702e-01.toFloat

    val embedding1 = w2v.makeCompositeVector(filteredDatasetTags).map(x => x*w1)
    val embedding2 = w2v.makeCompositeVector(filteredDatasetDescription).map(x => x*w2)
    val embedding3 = w2v.makeCompositeVector(filteredVariableTags).map(x => x*w3)
    val embedding4 = w2v.makeCompositeVector(filteredVariableDescription).map(x => x*w4)

    val embedding = Array(embedding1, embedding2, embedding3, embedding4).transpose.map(_.sum)
    WordEmbeddingMap.norm(embedding)

    embedding
  }
}

object DatamartWeightedAverageEmbedder {
  val stopwords = Set(
    "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no",
    "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", "this",
    "to", "was", "will", "with"
  )
}
