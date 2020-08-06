package org.clulab.alignment.indexer.lucene

import java.nio.file.Paths

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.Sourcer
import org.clulab.alignment.utils.TsvReader

object LuceneIndexerApp extends App {

  def newIndexWriter(dir: String): IndexWriter = {
    val analyzer = new StandardAnalyzer()
    val config = new IndexWriterConfig(analyzer)
    val index = FSDirectory.open(Paths.get(dir))

    new IndexWriter(index, config)
  }

  def indexDatamart(): Unit = {
    val luceneDir = "../lucene"
    val filename = "../datamarts.tsv"
    val tsvReader = new TsvReader()

    def newDocument(datamartId: String, datasetId: String, variableId: String, variableName: String, variableDescription: String): Document = {
      val document = new Document()

      document.add(new StoredField("datamartId", datamartId))
      document.add(new StoredField("datasetId", datasetId))
      document.add(new StoredField("variableId", variableId))

      document.add(new TextField("variableName", variableName, Field.Store.NO))
      document.add(new TextField("variableDescription", variableDescription, Field.Store.NO))

      document
    }

    newIndexWriter(luceneDir).autoClose { indexWriter =>
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

  indexDatamart()
}
