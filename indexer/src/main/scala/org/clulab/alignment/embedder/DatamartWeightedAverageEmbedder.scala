package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

class DatamartWeightedAverageEmbedder(w2v: CompactWordEmbeddingMap, weights: Array[Float]) extends DatamartEmbedder(w2v) {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val filteredDatasetTags = filter(datamartEntry.datasetTags)
    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    val embeddings = Array(
      scale(makeCompositeVector(filteredDatasetTags), weights(0)),
      scale(makeCompositeVector(filteredDatasetDescription), weights(1)),
      scale(makeCompositeVector(filteredVariableTags), weights(2)),
      scale(makeCompositeVector(filteredVariableDescription), weights(3))
    )
    val embedding = embeddings.transpose.map(_.sum)

    WordEmbeddingMap.norm(embedding)
    embedding
  }
}
