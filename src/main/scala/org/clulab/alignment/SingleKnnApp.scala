package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.Normalizer
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.searcher.lucene.LuceneSearcher
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

// This app gets the starting vector from the result of a Knn search.
// Lucene isn't involved at all.  In order to avoid using Word2Vec with
// its startup time and memory overhead, the glove index is used instead.
object SingleKnnApp extends App {
  val datamartFilename = "../hnswlib-datamart.idx"
  val gloveFilename = "../hnswlib-glove.idx"
  val luceneDirname = "../lucene-datamart"

  val maxHits = 10
  val queryString = "food"

  val datamartIndex = DatamartIndex.load(datamartFilename)
  val gloveIndex = GloveIndex.load(gloveFilename)
  val luceneSearcher = new LuceneSearcher(luceneDirname, "")

  val tokenizer = Tokenizer()
  val normalizer = Normalizer()
  val words = tokenizer.tokenize(queryString)
  val vector = {
    val composite = new Array[Float](GloveIndex.dimensions)

    words.foreach { word =>
      val vectorOpt = GloveIndex.find(gloveIndex, word)

      vectorOpt.foreach { vector =>
        vector.indices.foreach { index =>
          composite(index) += vector(index)
        }
      }
    }
    normalizer.normalize(composite)
    composite
  }
  val nearest = DatamartIndex.findNearest(datamartIndex, vector, maxHits).toSeq

  luceneSearcher.withReader { reader =>
    nearest.foreach { case SearchResult(item, knnValue) =>
      val document = luceneSearcher.find(reader, item.id)
      val datamartDocument = new DatamartDocument(document)
      val datamartIdentifier = item.id
      val datamartId = datamartIdentifier.datamartId
      val datasetId = datamartIdentifier.datasetId
      val variableId = datamartIdentifier.variableId
      val variableDescription = datamartDocument.variableDescription

      println(s"$datamartId\t$datasetId\t$variableId\t$variableDescription")
    }
  }
}
