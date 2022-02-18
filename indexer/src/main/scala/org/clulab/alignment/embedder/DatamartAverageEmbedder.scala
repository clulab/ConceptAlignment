package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

class DatamartAverageEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder(w2v) {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val filteredDatasetTags = filter(datamartEntry.datasetTags)
    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    val embeddings = Array(
      makeCompositeVector(filteredDatasetTags),
      makeCompositeVector(filteredDatasetDescription),
      makeCompositeVector(filteredVariableTags),
      makeCompositeVector(filteredVariableDescription)
    )
    val embedding = embeddings.transpose.map(_.sum / 4)

    WordEmbeddingMap.norm(embedding)
    embedding
  }
}
