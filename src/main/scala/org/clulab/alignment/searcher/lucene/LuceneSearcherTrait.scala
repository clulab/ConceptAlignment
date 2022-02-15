package org.clulab.alignment.searcher.lucene

import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.TermQuery
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

trait LuceneSearcherTrait {

  def search(queryString: String, maxHits: Int): Iterator[(Float, Document)]

  def search(geography: Seq[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Seq[DatamartIdentifier]

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)]

  def withReader[T](f: DirectoryReader => T): T

  def find(reader: DirectoryReader, identifier: DatamartIdentifier): Document
}
