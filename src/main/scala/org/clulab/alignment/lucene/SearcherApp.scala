package org.clulab.alignment.lucene

import java.nio.file.Paths

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.FSDirectory
import org.clulab.wm.eidos.utils.Closer.AutoCloser

object SearcherApp extends App {
  val luceneDir = "../lucene"
  val field = "variableDescription"
  val query = "camel"
  val maxHits = 100
  val collector = TopScoreDocCollector.create(maxHits)

  def newReader(): DirectoryReader = {
    DirectoryReader.open(FSDirectory.open(Paths.get(luceneDir)))
  }

  def newQuery(): Query = {
    val analyzer = new StandardAnalyzer()

    new QueryParser(field, analyzer).parse(query)
  }

  newReader().autoClose { reader =>
    val searcher = new IndexSearcher(reader)
    val query = newQuery()

    searcher.search(query, collector)

    val hits: Array[ScoreDoc] = collector.topDocs().scoreDocs

    hits.foreach { hit =>
      val docId = hit.doc
      val score = hit.score
      val document = searcher.doc(docId)
      val datamartId = document.get("datamartId")
      val datasetId = document.get("datasetId")
      val variableId = document.get("variableId")

      println(s"$datamartId/$datasetId/$variableId = $score")
    }
  }
}
