package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartWordEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder(w2v) {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val words = datamartEntry.words
    val embedding = makeCompositeVector(words)

    embedding
  }
}
