package org.clulab.alignment.searcher.lucene

import java.nio.file.Paths
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, LongPoint}
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.{BooleanClause, BooleanQuery, IndexSearcher, Query, ScoreDoc, TermQuery, TopScoreDocCollector}
import org.apache.lucene.store.FSDirectory
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.Closer.AutoCloser

class LuceneSearcher(luceneDirname: String, field: String) extends LuceneSearcherTrait {

  def getNumDocs: Int = {
    withReader { reader =>
      reader.numDocs()
    }
  }

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

      val hits = collector.topDocs().totalHits
      val scoreDocs = collector.topDocs().scoreDocs
      val scoresAndDocs = scoreDocs.map { hit => (hit.score, searcher.doc(hit.doc)) }

      scoresAndDocs.iterator
    }
  }

  def search(geography: Seq[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Seq[DatamartIdentifier] = {
    // If these are all empty, then there is not point to this expensive search.
    require(geography.nonEmpty || periodGteOpt.nonEmpty || periodLteOpt.nonEmpty)
    val builder = new BooleanQuery.Builder()

    geography.foreach { value =>
      val query = new TermQuery(new Term("geography", value.toLowerCase))

      builder.add(query, BooleanClause.Occur.MUST)
    }
    val gteQueryOpt = periodGteOpt.map { periodGte =>
      // The data might have neither, either, or both of these periods
      // If either is to the future of the query's periodGte, then there is overlap.
      val gteQuery = LongPoint.newRangeQuery("periodGte", periodGte, Long.MaxValue)
      val lteQuery = LongPoint.newRangeQuery("periodLte", periodGte, Long.MaxValue)
      val gteBooleanClause = new BooleanClause(gteQuery, BooleanClause.Occur.SHOULD)
      val lteBooleanClause = new BooleanClause(lteQuery, BooleanClause.Occur.SHOULD)

      val booleanBuilder = new BooleanQuery.Builder()
      booleanBuilder.add(gteBooleanClause)
      booleanBuilder.add(lteBooleanClause)
      booleanBuilder.setMinimumNumberShouldMatch(1)
      booleanBuilder.build()
    }
    gteQueryOpt.foreach { query => builder.add(query, BooleanClause.Occur.MUST) }

    val lteQueryOpt = periodLteOpt.map { periodLte =>
      // The data might have neither, either, or both of these periods
      // If either is to the future of the query's periodGte, then there is overlap.
      val gteQuery = LongPoint.newRangeQuery("periodGte", Long.MinValue, periodLte)
      val lteQuery = LongPoint.newRangeQuery("periodLte", Long.MinValue, periodLte)
      val gteBooleanClause = new BooleanClause(gteQuery, BooleanClause.Occur.SHOULD)
      val lteBooleanClause = new BooleanClause(lteQuery, BooleanClause.Occur.SHOULD)

      val booleanBuilder = new BooleanQuery.Builder()
      booleanBuilder.add(gteBooleanClause)
      booleanBuilder.add(lteBooleanClause)
      booleanBuilder.setMinimumNumberShouldMatch(1)
      booleanBuilder.build()
    }
    lteQueryOpt.foreach { query => builder.add(query, BooleanClause.Occur.MUST) }

    val query = builder.build()

    // TODO: withSearcher
    val datamartIdentifiers = withReader { reader =>
      val searcher = new IndexSearcher(reader)
      val topDocs = searcher.search(query, getNumDocs)

      if (topDocs.totalHits > 0) {
        topDocs.scoreDocs.map { scoreDoc =>
          val doc = scoreDoc.doc
          val document = searcher.doc(doc)

          new DatamartDocument(document).datamartIdentifier
        }
      }
      else
        Array.empty[DatamartIdentifier]
    }

    datamartIdentifiers
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

    // Does reader need to be closed?
    new LuceneIterator(newReader(), query, maxHits, pageSize)
  }

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)] = {
    val scoreDocs = search(queryString, maxHits)

    scoreDocs.map { case (score, document) => (score, new DatamartDocument(document)) }
  }
}
