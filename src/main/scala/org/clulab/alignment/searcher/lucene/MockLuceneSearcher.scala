package org.clulab.alignment.searcher.lucene

import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

class MockLuceneSearcher {

  def search(queryString: String, maxHits: Int): Iterator[(Float, Document)] =
      Seq((1f, new Document)).toIterator

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)] =
    Seq((1f, new DatamartDocument(new Document()))).toIterator

  def withReader[T](f: DirectoryReader => T): T

  def find(reader: DirectoryReader, identifier: DatamartIdentifier): Document = new Document()
    // Make sure the document has the right identifier.

}
