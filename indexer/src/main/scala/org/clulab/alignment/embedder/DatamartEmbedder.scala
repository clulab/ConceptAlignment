package org.clulab.alignment.embedder

import org.clulab.alignment.data.datamart.DatamartEntry

trait DatamartEmbedder {
  def embed(datamartEntry: DatamartEntry): Array[Float]
}
