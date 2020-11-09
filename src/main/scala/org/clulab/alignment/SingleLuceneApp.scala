package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.OntologyIndex
import org.clulab.alignment.searcher.lucene.LuceneSearcher
import org.clulab.alignment.searcher.lucene.LuceneSearcherTrait

// This app gets the starting vector from the result of a Lucene search.
object SingleLuceneApp extends App {
  val luceneDirname = "../lucene-datamart"
  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-wm_flattened.idx"

  val field = "variableDescription"
  val maxHits = 10
  val queryString = "food"

  val luceneSearcher: LuceneSearcherTrait = new LuceneSearcher(luceneDirname, field)
  val datamartIndex = DatamartIndex.load(datamartFilename)
  val ontologyIndex = OntologyIndex.load(ontologyFilename)

  // In this example, just take the single best search result.
  val datamartDocumentOpt = {
    val results = luceneSearcher.datamartSearch(queryString, 1)

    if (results.hasNext)
      Some(results.next._2)
    else
      None
  }
  val datamartItemOpt = datamartDocumentOpt.flatMap { datamartDocument =>
    val id = DatamartIdentifier(datamartDocument.datamartId, datamartDocument.datasetId, datamartDocument.variableId)
    val datamartAlignmentItemOpt = datamartIndex.get(id)

    datamartAlignmentItemOpt
  }

  datamartItemOpt.map { datamartItem =>
    val id = datamartItem.id
    val vector = datamartItem.vector

    println(s"$id =>")
    ontologyIndex
        .findNearest(vector, maxHits)
        .sortBy(-_.distance)
        .foreach { case SearchResult(item, distance) =>
          println(s"$item $distance")
        }
  }
}
