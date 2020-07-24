package org.clulab.alignment.knn.hnswlib

import com.github.jelmerk.knn.scalalike._
import com.github.jelmerk.knn.scalalike.hnsw._

object Indexer extends App {

  case class Word(id: String, vector: Array[Float]) extends Item[String, Array[Float]] {
    override def dimensions(): Int = vector.length
  }

  val dimensions = 4
  val words = Array(
    Word("one",   Array(1f, 2f, 3f, 4f)),
    Word("two",   Array(2f, 3f, 4f, 5f)),
    Word("three", Array(3f, 4f, 5f, 6f))
  )
  val index = HnswIndex[String, Array[Float], Word, Float](dimensions, floatCosineDistance, words.size)

  index.addAll(words)
  // This finds neighbors of specific, known item.
  index.findNeighbors("three", k = 10).foreach { case SearchResult(item, distance) =>
    println(s"$item $distance")
  }

  // This finds neighbors based on location that doesn't necessarily correspond to any known item.

}
