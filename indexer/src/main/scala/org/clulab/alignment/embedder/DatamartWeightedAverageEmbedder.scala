package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMap}

class DatamartWeightedAverageEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val filteredDatasetTags = filter(datamartEntry.datasetTags)
    val filteredDatasetDescription = filter(datamartEntry.datasetDescription)
    val filteredVariableTags = filter(datamartEntry.variableTags)
    val filteredVariableDescription = filter(datamartEntry.variableDescription)

    val embeddings = Array(
      w2v.makeCompositeVector(filteredDatasetTags).map(_ * DatamartWeightedAverageEmbedder.w1),
      w2v.makeCompositeVector(filteredDatasetDescription).map(_ * DatamartWeightedAverageEmbedder.w2),
      w2v.makeCompositeVector(filteredVariableTags).map(_ * DatamartWeightedAverageEmbedder.w3),
      w2v.makeCompositeVector(filteredVariableDescription).map(_ * DatamartWeightedAverageEmbedder.w4)
    )
    val embedding = embeddings.transpose.map(_.sum)

    WordEmbeddingMap.norm(embedding)
    embedding
  }
}

object DatamartWeightedAverageEmbedder {
  val w1 = 6.14417366e-06.toFloat
  val w2 = 1.12534466e-07.toFloat
  val w3 = 4.13991165e-08.toFloat
  val w4 = 9.99993702e-01.toFloat
}