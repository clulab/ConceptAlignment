package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.data.Normalizer
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex.Index
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem
import org.clulab.alignment.searcher.lucene.LuceneSearcher
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

// This app gets the starting vector from the result of a Knn search.
// Lucene isn't involved in that.  In order to avoid using Word2Vec with
// its startup time and memory overhead, the glove index is used instead.
// Lucene is involved at the end to retrieve remaining parts of the
// datamart entry that can't be stored in the Knn index.
class SingleKnnApp() {
  import Locations._

  val datamartIndex: Index = DatamartIndex.load(datamartFilename)
  val luceneSearcher = new LuceneSearcher(luceneDirname, "")
  val gloveIndex: GloveIndex.Index = GloveIndex.load(gloveFilename)

  def getVectorOpt(queryString: String): Option[Array[Float]] = {
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
      val len = normalizer.length(composite)
      if (len != 0) Some(normalizer.normalize(composite))
      else None
    }

    vector
  }

  def getDatamartDocuments(searchResults: Seq[SearchResult[DatamartAlignmentItem, Float]]): Seq[(DatamartDocument, Float)] = {
    val datamartDocuments = luceneSearcher.withReader { reader =>
      searchResults.map { case SearchResult(item, score) =>
        val document = luceneSearcher.find(reader, item.id)
        val datamartDocument = new DatamartDocument(document)

        (datamartDocument, score)
      }.toArray // must be retrieved before reader is closed
    }

    datamartDocuments
  }

  def run(queryString: String, maxHits: Int): Seq[(DatamartDocument, Float)] = {
    val vectorOpt: Option[Array[Float]] = getVectorOpt(queryString)
    val searchResults: Seq[SearchResult[DatamartAlignmentItem, Float]] = vectorOpt.map { vector =>
      DatamartIndex.findNearest(datamartIndex, vector, maxHits).toSeq
    }.getOrElse(Seq.empty)
    val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = getDatamartDocuments(searchResults)

    datamartDocumentsAndScores
  }
}

object Locations {
  val datamartFilename = "../hnswlib-datamart.idx"
  val gloveFilename = "../hnswlib-glove.idx"
  val luceneDirname = "../lucene-datamart"
}

object SingleKnnApp extends App {
  val datamartDocumentsAndScores = new SingleKnnApp().run("food", 10)

  datamartDocumentsAndScores.foreach { case (datamartDocument, score) =>
    println(s"${datamartDocument.datamartId}\t${datamartDocument.datasetId}\t${datamartDocument.variableId}\t${datamartDocument.variableName}\t${datamartDocument.variableDescription}\t$score")
  }
}
