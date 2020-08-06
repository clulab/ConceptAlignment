package org.clulab.alignment.searcher.lucene

object SearcherApp extends App {
  val luceneDir = "../lucene"
  val field = "variableDescription"
  val queryString = "camel"
  val maxHits = 100

  val searcher = new Searcher(luceneDir, field)
  val scoresAndDocuments = searcher.search(queryString, maxHits)

  scoresAndDocuments.foreach { case (score, document) =>
    val datamartId = document.get("datamartId")
    val datasetId = document.get("datasetId")
    val variableId = document.get("variableId")

    println(s"$datamartId/$datasetId/$variableId = $score")
  }
}
