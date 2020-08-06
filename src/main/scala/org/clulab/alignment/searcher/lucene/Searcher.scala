package org.clulab.alignment.searcher.lucene

import java.nio.file.Paths

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.FSDirectory
import org.clulab.alignment.utils.Closer.AutoCloser

class Searcher(luceneDir: String, field: String) {

  def newReader(): DirectoryReader = {
    DirectoryReader.open(FSDirectory.open(Paths.get(luceneDir)))
  }

  def newQuery(query: String): Query = {
    val analyzer = new StandardAnalyzer()

    new QueryParser(field, analyzer).parse(query)
  }

  def search(queryString: String, maxHits: Int): Array[(Float, Document)] = {
    val collector = TopScoreDocCollector.create(maxHits)
    val query = newQuery(queryString)

    newReader().autoClose { reader =>
      val searcher = new IndexSearcher(reader)
      searcher.search(query, collector)

      val scoreDocs = collector.topDocs().scoreDocs
      scoreDocs.map { hit => (hit.score, searcher.doc(hit.doc)) }
    }
  }
}
