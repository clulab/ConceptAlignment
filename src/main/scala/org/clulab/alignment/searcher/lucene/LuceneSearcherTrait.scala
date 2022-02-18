package org.clulab.alignment.searcher.lucene

import org.apache.lucene.document.Document
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

trait LuceneSearcherTrait {

  def documentSearch(queryString: String, maxHits: Int): Iterator[(Float, Document)]

  def datamartSearch(queryString: String, maxHits: Int): Iterator[(Float, DatamartDocument)]

  def search(geography: Seq[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Seq[DatamartIdentifier]

  def find(identifier: DatamartIdentifier): Document

  def close(): Unit
}
