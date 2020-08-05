package org.clulab.alignment.knn.hnswlib

import java.io.File

import ai.lum.common.ConfigUtils
import com.github.jelmerk.knn.scalalike._
import com.github.jelmerk.knn.scalalike.hnsw._
import org.clulab.alignment.grounder.datamart.DatamartOntology
import org.clulab.alignment.grounder.datamart.DatamartTokenizer
import org.clulab.alignment.searcher.knn.hnswlib.DatamartAlignmentItem
import org.clulab.alignment.searcher.knn.hnswlib.GloveAlignmentItem
import org.clulab.alignment.searcher.knn.hnswlib.OntologyAlignmentItem
import org.clulab.alignment.searcher.knn.hnswlib.TestAlignmentItem
import org.clulab.alignment.utils.{OntologyHandlerHelper => OntologyHandler}
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.EidosOntologyGrounder

object IndexerApp extends App {
  lazy val w2v = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
  val dimensions = 300

  // This is just for testing.
  def indexTest(): Unit = {
    val filename = "../hnswlib-test.idx"
    val dimensions = 4
    val items = Array(
      TestAlignmentItem("one",   Array(1f, 2f, 3f, 4f)),
      TestAlignmentItem("two",   Array(2f, 3f, 4f, 5f)),
      TestAlignmentItem("three", Array(3f, 4f, 5f, 6f))
    )
    val index = HnswIndex[String, Array[Float], TestAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index.save(new File(filename))
  }

  // There isn't much call for this.  It is mostly to test the indexer with a large number of entries.
  def indexGlove(): Unit = {
    val filename = "../hnswlib-glove.idx"
    val keys = w2v.keys
    val index = HnswIndex[String, Array[Float], GloveAlignmentItem, Float](dimensions, floatCosineDistance, keys.size)
    val items = keys.map { key => GloveAlignmentItem(key, w2v.get(key).get) }

    index.addAll(items)
    index.save(new File(filename))
  }

  def indexOntology(): Unit = {
    val namespace = "wm_flattened"
    val ontologyHandler = OntologyHandler.fromConfig()
    val eidosOntologyGrounder = ontologyHandler.ontologyGrounders
        .collect { case grounder: EidosOntologyGrounder => grounder}
        .find { grounder => grounder.name == namespace }
        .get
    val conceptEmbeddings = eidosOntologyGrounder.conceptEmbeddings
    val filename = s"../hnswlib-$namespace.idx"
    val items = conceptEmbeddings.map { conceptEmbedding =>
      val name = conceptEmbedding.namer.name
      val concept = conceptEmbedding.embedding

      OntologyAlignmentItem(name, concept)
    }
    val index = HnswIndex[String, Array[Float], OntologyAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index.save(new File(filename))
  }

  def indexDatamart(): Unit = {
    val filename = "../hnswlib-datamart.idx"
    val tokenizer = DatamartTokenizer()
    val ontology = DatamartOntology.fromFile("../datamarts.tsv", tokenizer)
    val index = HnswIndex[String, Array[Float], DatamartAlignmentItem, Float](dimensions, floatCosineDistance, ontology.size)
    val items = ontology.datamartEntries.map { datamartEntry =>
      val identifier = datamartEntry.identifier
      val words = datamartEntry.words
      val embedding = w2v.makeCompositeVector(words)

      DatamartAlignmentItem(identifier.toString, embedding)
    }

    index.addAll(items)
    index.save(new File(filename))
  }

  indexTest()
//  indexGlove()
  indexOntology()
//  indexDatamart()
}
