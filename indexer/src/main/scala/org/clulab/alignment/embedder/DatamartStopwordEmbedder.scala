package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.embeddings.CompactWordEmbeddingMap

class DatamartStopwordEmbedder(w2v: CompactWordEmbeddingMap) extends DatamartEmbedder {

  def embed(datamartEntry: DatamartEntry): Array[Float] = {
    val words = datamartEntry.words
    val filteredWords = filter(words)
    val embedding = w2v.makeCompositeVector(filteredWords)

    embedding
  }
}
