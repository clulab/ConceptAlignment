package org.clulab.alignment.searcher.lucene.document

import org.apache.lucene.document.Document

class DatamartDocument(document: Document) {

  def datamartId = document.get("datamartId")

  def datasetId = document.get("datasetId")

  def variableId = document.get("variableId")
}
