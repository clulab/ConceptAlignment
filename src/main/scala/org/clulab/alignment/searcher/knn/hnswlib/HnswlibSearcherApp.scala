package org.clulab.alignment.searcher.knn.hnswlib

import com.github.jelmerk.knn.scalalike.SearchResult
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.OntologyIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.SampleIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.GloveAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.OntologyAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.SampleAlignmentItem

import scala.util.Random

object HnswlibSearcherApp extends App {
  val random = new Random(0)

  def normalize(array: Array[Float]): Unit = {
    val length2 = array.foldLeft(0f) { case (total: Float, item: Float) => total + item * item }
    val length = math.sqrt(length2).toFloat

    array.indices.foreach { index => array(index) /= length }
  }

  def newVector(): Array[Float] = {
    val vector = new Array[Float](300)

    vector.indices.foreach { index => vector(index) = random.nextFloat() }
    normalize(vector)
    vector
  }

  def searchSample(): Unit = {
    val filename = "../hnswlib-sample.idx"
    val item = SampleAlignmentItem("three", Array(3f, 4f, 5f, 6f))
    val index = SampleIndex.load(filename)

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
    val vector = newVector()
    val index = GloveIndex.load(filename)

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(vector, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

  def searchOntology(): Unit = {
    val filename = "../hnswlib-wm_flattened.idx"
    val vector = newVector()
    val index = OntologyIndex.load(filename)

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(vector, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

  def searchDatamart(): Unit = {
    val filename = "../hnswlib-datamart.idx"
    val vector = newVector()
    val index = DatamartIndex.load(filename)

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    index.findNearest(vector, k = 10).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

  def infiniteSearchDatamart(): Unit = {
    val filename = "../hnswlib-datamart.idx"
    val vector = newVector()
    val index = DatamartIndex.load(filename)
    val size = index.size

    // This finds neighbors based on location that doesn't necessarily correspond to any known item.
    DatamartIndex.findNearest(index, vector).foreach { case SearchResult(item, distance) =>
      println(s"$item $distance")
    }
  }

//  searchSample()
//  searchGlove()
//  searchOntology()
//  searchDatamart()

  infiniteSearchDatamart()
}
