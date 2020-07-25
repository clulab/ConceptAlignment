package org.clulab.alignment.knn.hnswlib

import java.io.File

import com.github.jelmerk.knn.scalalike._
import com.github.jelmerk.knn.scalalike.hnsw._
import org.clulab.embeddings.word2vec.CompactWord2Vec

object IndexerApp extends App {

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

  def indexGlove(): Unit = {
    val filename = "../hnswlib-glove.idx"
    val dimensions = 300
    val w2v = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
    val keys = w2v.keys
    val index = HnswIndex[String, Array[Float], GloveAlignmentItem, Float](dimensions, floatCosineDistance, keys.size)
    val items = keys.map { key => GloveAlignmentItem(key, w2v.get(key).get) }

    index.addAll(items)
    index.save(new File(filename))
  }

  def indexOntology(): Unit = {
    // This is waiting for easier access to the ontologies.
    val filename = "../hnswlib-ontology.idx"
    val dimensions = 300
    val items = Array.empty[OntologyAlignmentItem]
    val index = HnswIndex[String, Array[Float], OntologyAlignmentItem, Float](dimensions, floatCosineDistance, items.size)

    index.addAll(items)
    index.save(new File(filename))
  }

  indexTest()
  indexGlove()
  indexOntology()
}
