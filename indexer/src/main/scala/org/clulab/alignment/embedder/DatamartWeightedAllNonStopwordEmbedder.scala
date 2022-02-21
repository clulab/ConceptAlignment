package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

class DatamartWeightedAllNonStopwordEmbedder(w2v: CompactWordEmbeddingMap, weights: Array[Float]) extends DatamartEmbedder(w2v) {

  def mutableAddWeighted(left: Array[Float], right: IndexedSeq[Float], weight: Float): Array[Float] = {
    var i = 0 // optimization

    while (i < left.length) {
      left(i) += (right(i) * weight)
      i += 1
    }
    left
  }

  def mutableAddWeighted(left: Array[Float], words: Array[String], weight: Float): Array[Float] = {
    val rights = words.flatMap(w2v.get)

    rights.foldLeft(left) { case (result, array) => mutableAddWeighted(result, array, weight) }
  }

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val filteredDatasetTags = filter(datamartEntry.datasetTags)
    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    val step0 = new Array[Float](300)
    val step1 = mutableAddWeighted(step0, filteredDatasetTags, weights(0))
    val step2 = mutableAddWeighted(step1, filteredDatasetDescription, weights(1))
    val step3 = mutableAddWeighted(step2, filteredVariableTags, weights(2))
    val step4 = mutableAddWeighted(step3, filteredVariableDescription, weights(3))
    val embedding = step4

    WordEmbeddingMap.norm(embedding)
    embedding
  }
}

object DatamartWeightedAllNonStopwordEmbedder {
  val weights = DatamartOptWeightedAverageEmbedder.weights

  def apply(w2v: CompactWordEmbeddingMap): DatamartWeightedAllNonStopwordEmbedder = new DatamartWeightedAllNonStopwordEmbedder(w2v, weights)
}
