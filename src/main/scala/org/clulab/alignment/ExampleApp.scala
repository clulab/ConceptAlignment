package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.OntologyIndex
import org.clulab.alignment.searcher.lucene.LuceneSearcher

object ExampleApp extends App {
  val luceneDir = "../lucene"
  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-wm_flattened.idx"

  val field = "variableDescription"
  val maxHits = 10
  val queryString = "food"

  val luceneSearcher = new LuceneSearcher(luceneDir, field)
  val datamartIndex = DatamartIndex.load(datamartFilename)
  val ontologyIndex = OntologyIndex.load(ontologyFilename)

  // In this example, just take the single best search result.
  val datamartDocumentOpt = luceneSearcher.datamartSearch(queryString, 1).headOption.map(_._2)
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
