package org.clulab.alignment.searcher.lucene

import java.nio.file.Paths

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.FSDirectory
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.Closer.AutoCloser

class LuceneSearcher(luceneDirname: String, field: String) extends LuceneSearcherTrait {

  def newReader(): DirectoryReader = {
    val path = Paths.get(luceneDirname)
    val fsDirectory = FSDirectory.open(path)

    DirectoryReader.open(fsDirectory)
  }

  def newQuery(query: String): Query = {
    val analyzer = new StandardAnalyzer()

    new QueryParser(field, analyzer).parse(query)
  }

  def search(queryString: String, maxHits: Int): Iterator[(Float, Document)] = {
    val collector = TopScoreDocCollector.create(maxHits)
    val query = newQuery(queryString)

    newReader().autoClose { reader =>
      val searcher = new IndexSearcher(reader)
      searcher.search(query, collector)

      val scoreDocs = collector.topDocs().scoreDocs
      val scoresAndDocs = scoreDocs.map { hit => (hit.score, searcher.doc(hit.doc)) }

      scoresAndDocs.iterator
    }
  }

  def withReader[T](f: DirectoryReader => T): T = {
    val result = newReader().autoClose { reader =>
      f(reader)
    }

    result
  }

  def find(reader: DirectoryReader, identifier: DatamartIdentifier): Document = {
    val query = new TermQuery(new Term("id", identifier.toString))
    val searcher = new IndexSearcher(reader)
    val topDocs = searcher.search(query, 1)

    assert(topDocs.totalHits > 0)

    val doc = topDocs.scoreDocs.head.doc
    val document = searcher.doc(doc)

    document
  }

  class LuceneIterator(reader: DirectoryReader, query: Query, maxHits: Int, pageSize: Int) extends Iterator[(Float, Document)] {
/*    var opened = true
    val collector = TopScoreDocCollector.create(math.min(pageSize, maxHits), null)
    val scoreDocs = collector.topDocs(0, ).scoreDocs
    val searcher = new IndexSearcher(reader)

    scoreDocs.map { hit => (hit.score, searcher.doc(hit.doc)) }
    searcher.search(query, collector)
    val scoresAndDocs = scoreDocs.map { hit => (hit.score, searcher.doc(hit.doc)) }
    new LuceneIterator(collector)

    def close(): Unit = {
      if (opened)
        reader.close()
      opened = false
    }*/
    override def hasNext: Boolean = ???

    override def next(): (Float, Document) = ???
  }

  def infiniteSearch(queryString: String, maxHits: Int, pageSize: Int): Iterator[(Float, Document)] = {
    val query = newQuery(queryString)

    new LuceneIterator(newReader(), query, maxHits, pageSize)
  }

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)] = {
    val scoreDocs = search(queryString, maxHits)

    scoreDocs.map { case (score, document) => (score, new DatamartDocument(document)) }
  }
}
