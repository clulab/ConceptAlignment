package org.clulab.alignment.searcher.lucene

import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

class NullLuceneSearcher {

  def search(queryString: String, maxHits: Int): Iterator[(Float, Document)] = Iterator.empty

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)] = Iterator.empty

  def withReader[T](f: DirectoryReader => T): T = null.asInstanceOf[T]

  def find(reader: DirectoryReader, identifier: DatamartIdentifier): Document = new Document
}
