package org.clulab.alignment.groundings.ontologies

import org.clulab.alignment.groundings.DatamartNamer
import org.clulab.alignment.groundings.DatamartParser
import org.clulab.wm.eidos.groundings.EidosWordToVec
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import org.clulab.wm.eidos.utils.Namer
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.TsvReader

class DatamartOntology(namersAndValues: Seq[(Namer, Array[String])]) {

  def size: Integer =  namersAndValues.size

  def getNamer(n: Integer): Namer = namersAndValues(n)._1

  def getValues(n: Integer): Array[String] = namersAndValues(n)._2
}

object DatamartOntology {

  def fromFile(filename: String, word2Vec: EidosWordToVec, parser: DatamartParser): DatamartOntology = {
    val tsvReader = new TsvReader()
    val namersAndValues = Sourcer.sourceFromFile(filename).autoClose { source =>
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
        val namer = new DatamartNamer(datamartId, datasetId, variableId)
        val words = parser.parse(variableDescription)

        (namer, words)
      }.toVector
    }

    new DatamartOntology(namersAndValues)
  }
}
