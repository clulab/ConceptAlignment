package org.clulab.alignment.grounder.datamart

import org.clulab.alignment.datamart.DatamartEntry
import org.clulab.alignment.datamart.DatamartIdentifier
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.Sourcer
import org.clulab.alignment.utils.TsvReader

class DatamartOntology(val datamartEntries: Seq[DatamartEntry]) {

  def size: Int = datamartEntries.size
}

object DatamartOntology {

  def fromFile(filename: String, tokenizer: DatamartTokenizer): DatamartOntology = {
    val tsvReader = new TsvReader()
    val datamartEntries = Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.buffered.drop(1).map { line =>
        val Array(
          datamartId,
          datasetId,
          _,
          _,
          _,
          variableId,
          _,
          variableDescription
        ) = tsvReader.readln(line, length = 8)
        val datamartIdentifier = new DatamartIdentifier(datamartId, datasetId, variableId)
        val words = tokenizer.tokenize(variableDescription)

        DatamartEntry(datamartIdentifier, words)
      }.toVector
    }

    new DatamartOntology(datamartEntries)
  }
}
