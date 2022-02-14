package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartSingleEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder(w2v) {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
//    val filteredDatasetTags = filter(datamartEntry.datasetTags)
//    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
//    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    // Return just one of the above.
    val embedding = makeCompositeVector(filteredVariableTags)

    embedding
  }
}
