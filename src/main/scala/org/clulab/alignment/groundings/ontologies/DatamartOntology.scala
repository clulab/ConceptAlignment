package org.clulab.alignment.groundings.ontologies

import org.clulab.alignment.groundings.DatamartEntry
import org.clulab.alignment.groundings.DatamartIdentifier
import org.clulab.alignment.groundings.DatamartParser
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.TsvReader

class DatamartOntology(val datamartEntries: Seq[DatamartEntry])

object DatamartOntology {

  def fromFile(filename: String, parser: DatamartParser): DatamartOntology = {
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
        val words = parser.parse(variableDescription)

        DatamartEntry(datamartIdentifier, words)
      }.toVector
    }

    new DatamartOntology(datamartEntries)
  }
}
