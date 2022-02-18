package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartAllNonStopwordEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder(w2v) {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val filteredDatasetTags = filter(datamartEntry.datasetTags)
    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    val filteredWords = filteredDatasetTags ++ filteredDatasetDescription ++ filteredVariableTags ++ filteredVariableDescription
    val embedding = makeCompositeVector(filteredWords)

    embedding
  }
}
