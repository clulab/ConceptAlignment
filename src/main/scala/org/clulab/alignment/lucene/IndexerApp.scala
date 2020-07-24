package org.clulab.alignment.lucene

import java.nio.file.Paths

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.TsvReader

object IndexerApp extends App {
  val filename = "../datamarts.tsv"
  val luceneDir = "../lucene"

  val tsvReader = new TsvReader()

  def newIndexWriter(): IndexWriter = {
    val analyzer = new StandardAnalyzer()
    val config = new IndexWriterConfig(analyzer)
    val index = FSDirectory.open(Paths.get(luceneDir))

    new IndexWriter(index, config)
  }

  def newDocument(datamartId: String, datasetId: String, variableId: String, variableName: String, variableDescription: String): Document = {
    val document = new Document()

    document.add(new StoredField("datamartId", datamartId))
    document.add(new StoredField( "datasetId", datasetId))
    document.add(new StoredField("variableId", variableId))

    document.add(new TextField("variableName",        variableName,        Field.Store.NO))
    document.add(new TextField("variableDescription", variableDescription, Field.Store.NO))

    document
  }

  newIndexWriter.autoClose { indexWriter =>
    Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.buffered.drop(1).foreach { line =>
        val Array(
          datamartId,
          datasetId,
          _,
          _,
          _,
          variableId,
          variableName,
          variableDescription
        ) = tsvReader.readln(line, length = 8)
        val document = newDocument(datamartId, datasetId, variableId, variableName, variableDescription)

        indexWriter.addDocument(document)
      }
    }
  }
}
