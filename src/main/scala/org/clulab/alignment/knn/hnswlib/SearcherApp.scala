package org.clulab.alignment.knn.hnswlib

import java.io.File

import com.github.jelmerk.knn.scalalike.SearchResult
import com.github.jelmerk.knn.scalalike.hnsw.HnswIndex

import scala.util.Random

object SearcherApp extends App {

  def normalize(array: Array[Float]): Unit = {
    val length2 = array.foldLeft(0f) { case (total: Float, item: Float) => total + item * item }
    val length = math.sqrt(length2).toFloat

    array.indices.foreach { index => array(index) /= length }
  }

  def searchTest(): Unit = {
    val filename = "../hnswlib-test.idx"
    val item = TestAlignmentItem("three", Array(3f, 4f, 5f, 6f))
    val index = HnswIndex.load[String, Array[Float], TestAlignmentItem, Float](new File(filename))

    // This finds neighbors of specific, known item.
    index.findNeighbors(item.id, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(Array(3f, 4f, 5f, 5f), k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

  def searchGlove(): Unit = {
    val filename = "../hnswlib-glove.idx"
    val index = HnswIndex.load[String, Array[Float], GloveAlignmentItem, Float](new File(filename))
    val random = new Random(0)
    val vector = new Array[Float](300)

    vector.indices.foreach { index => vector(index) = random.nextFloat() }
    normalize(vector)

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(vector, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

  def searchOntology(): Unit = {
    val filename = "../hnswlib-ontology.idx"
    val index = HnswIndex.load[String, Array[Float], OntologyAlignmentItem, Float](new File(filename))
    val random = new Random(0)
    val vector = new Array[Float](300)

    vector.indices.foreach { index => vector(index) = random.nextFloat() }
    normalize(vector)

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(vector, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }
}
