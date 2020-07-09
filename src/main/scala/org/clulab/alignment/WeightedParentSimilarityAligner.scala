package org.clulab.alignment

import org.clulab.embeddings.word2vec.Word2Vec
import org.clulab.processors.Processor

class WeightedParentSimilarityAligner(val w2v: Word2Vec, val proc: Processor) extends Aligner {

  val name = "WeightedParentSimilarity"
  private val separator: String = "/"

  override def align(c1: Concept, c2: Concept): Score = {
    // like entity/natural_resource/water ==> Seq(entity, natural_resource, water)
    // reverse: Seq(water, natural_resource, water)
    val parents1 = getParents(c1.name).reverse
    val parents2 = getParents(c2.name).reverse
    val k = math.min(parents1.length, parents2.length)
    var avg = 0.0f // optimization
    for (i <- 0 until k) {
      val score = mweStringSimilarity(parents1(i), parents2(i))
      avg += (score/ (i + 1)).toFloat
    }
    Score(name, avg)
  }

  private def getParents(conceptName: String): Seq[String] = {
    conceptName.split(separator)
  }

  def mweStringSimilarity(a: String, b: String): Double = {
    dotProduct(mkMWEmbedding(a), mkMWEmbedding(b))
  }

  def mkMWEmbedding(s: String, contentOnly: Boolean = false): Array[Double] = {
    val words = s.split("[ |_]").map(Word2Vec.sanitizeWord(_))
    w2v.makeCompositeVector(selectWords(words, contentOnly))
  }

  // Filter out non-content words or not
  private def selectWords(ws: Seq[String], contentOnly: Boolean): Seq[String] = {
    val tagSet = Set("NN", "VB", "JJ")

    if (contentOnly) {
      val doc = proc.mkDocument(ws.mkString(" "))
      proc.tagPartsOfSpeech(doc)
      for {
        (w, i) <- doc.sentences.head.words.zipWithIndex
        tag = doc.sentences.head.tags.get(i)
        if tagSet.exists(prefix => tag.startsWith(prefix)) && w != "provision" // todo: remove?
      } yield w
    }
    else {
      ws
    }
  }



}
