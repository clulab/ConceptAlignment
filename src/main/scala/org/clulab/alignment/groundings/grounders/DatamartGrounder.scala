package org.clulab.alignment.groundings.grounders

import org.clulab.alignment.groundings.DatamartParser
import org.clulab.alignment.groundings.Grounding
import org.clulab.alignment.groundings.ontologies.DatamartOntology
import org.clulab.wm.eidos.groundings.ConceptEmbedding
import org.clulab.wm.eidos.groundings.EidosWordToVec

class DatamartGrounder(parser: DatamartParser, ontology: DatamartOntology, word2vec: EidosWordToVec) {
  val conceptEmbeddings: Seq[ConceptEmbedding] = {
    0.until(ontology.size).map { index =>
      val namer = ontology.getNamer(index)
      val words = ontology.getValues(index)
      val embedding = word2vec.makeCompositeVector(words)

      ConceptEmbedding(namer, embedding)
    }
  }

  def ground(text: String, topK: Int): Seq[Grounding] = {
    val words = parser.parse(text)
    // This will return the topK based on the word2vec setting, which is hopefully greater than this value.
    val allSimilarities = word2vec.calculateSimilarities(words, conceptEmbeddings)
    // Sort highest to lowest value.  Ties cannot be broken because namer is not yet sortable.
    val sortedSimilarities = allSimilarities.sortBy(similarity => -similarity._2)
    val topKSimilarities = sortedSimilarities.take(topK)
    val groundings = topKSimilarities.map { similarity => Grounding(similarity._1, similarity._2) }

    groundings
  }
}
