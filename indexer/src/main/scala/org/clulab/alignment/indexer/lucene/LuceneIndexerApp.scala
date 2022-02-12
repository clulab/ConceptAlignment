package org.clulab.alignment.indexer.lucene

import java.nio.file.Paths
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, LongPoint, StoredField, StringField, TextField}
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.Sourcer
import org.clulab.alignment.utils.TsvReader
import ujson.{IncompleteParseException, ParseException}

class LuceneIndexerApp(luceneLocations: LuceneLocationsTrait) {

  def newIndexWriter(dir: String): IndexWriter = {
    val analyzer = new StandardAnalyzer()
    val config = new IndexWriterConfig(analyzer)
    val index = FSDirectory.open(Paths.get(dir))

    new IndexWriter(index, config)
  }

  def indexDatamart(): Unit = {
    val tsvReader = new TsvReader()

    def newDocument(datamartId: String, datasetId: String, datasetGeography: Seq[String],
        datasetPeriodGteOpt: Option[Long], datasetPeriodLteOpt: Option[Long],
        variableId: String, variableName: String, variableDescription: String): Document = {
      val identifier = DatamartIdentifier(datamartId, datasetId, variableId)
      val document = new Document()

      document.add(new StringField("id", identifier.toString(), Field.Store.NO))

      document.add(new StoredField("datamartId", datamartId))
      document.add(new StoredField("datasetId", datasetId))
      document.add(new StoredField("variableId", variableId))

      datasetGeography.foreach { geography =>
        document.add(new TextField("geography", geography, Field.Store.NO))
      }

      datasetPeriodGteOpt.foreach { datasetPeriodGte =>
        document.add(new LongPoint("periodGte", datasetPeriodGte))
      }
      datasetPeriodLteOpt.foreach { datasetPeriodLte =>
        document.add(new LongPoint("periodLte", datasetPeriodLte))
      }

      document.add(new TextField("variableName", variableName, Field.Store.YES))
      document.add(new TextField("variableDescription", variableDescription, Field.Store.YES))

      document
    }

    def geographyToStrings(json: String): Seq[String] = {
      try {
        ujson.read(json).arr.toIndexedSeq.map(_.str)
      }
      catch {
        // These can be thrown in memory-constrained situations even if the json is OK.
        case exception: IncompleteParseException => Seq.empty
        case exception: ParseException => Seq.empty
      }
    }

    newIndexWriter(luceneLocations.luceneDirname).autoClose { indexWriter =>
      Sourcer.sourceFromFile(luceneLocations.datamartFilename).autoClose { source =>
        source.getLines.buffered.drop(1).foreach { line =>
          val Array(
          datamartId,
          datasetId,
          _, // datasetName
          _, // datasetTags
          _, // datasetDescription
          _, // datasetUrl

          datasetGeography,
          datasetPeriodGte,
          datasetPeriodLte,

          variableId,
          variableName,
          _, // variableTags
          variableDescription
          ) = tsvReader.readln(line, length = 13)
          val datasetPeriodGteOpt = if (datasetPeriodGte.isEmpty) None else Some(datasetPeriodGte.toLong)
          val datasetPeriodLteOpt = if (datasetPeriodLte.isEmpty) None else Some(datasetPeriodLte.toLong)

          val document = newDocument(datamartId, datasetId, geographyToStrings(datasetGeography),
            datasetPeriodGteOpt, datasetPeriodLteOpt,
            variableId, variableName, variableDescription)

          indexWriter.addDocument(document)
        }
      }
    }
  }

  def run(): Unit = indexDatamart()
}

class StaticLuceneLocations(val datamartFilename: String, val luceneDirname: String) extends LuceneLocationsTrait {
}

object LuceneIndexerApp extends App {

  def run(datamartFilename: String, luceneDirname: String): Unit = {
    new LuceneIndexerApp(new StaticLuceneLocations(datamartFilename, luceneDirname)).run()
  }

  run(args(0), args(1))
}
