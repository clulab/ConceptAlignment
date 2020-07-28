package org.clulab.alignment.groundings.grounders

import org.clulab.alignment.Aligner
import org.clulab.alignment.ConceptSequence
import org.clulab.alignment.ScoredPair
import org.clulab.alignment.groundings.DatamartConcept
import org.clulab.alignment.groundings.DatamartParser
import org.clulab.alignment.groundings.ontologies.DatamartOntology
import org.clulab.alignment.utils.ConceptUtils
import org.clulab.embeddings.word2vec.CompactWord2Vec

class DatamartGrounder(ontology: DatamartOntology, word2vec: CompactWord2Vec, aligner: Aligner) {
  val parser = DatamartParser()
  val datamartConcepts = ontology.datamartEntries.map { datamartEntry =>
    val identifier = datamartEntry.identifier
    val words = datamartEntry.words
    // TODO: This might need to be done differently.
    val embedding = word2vec.makeCompositeVector(words).map(_.toFloat)

    DatamartConcept(identifier, embedding)
  }
  val conceptSequence = ConceptSequence(datamartConcepts)

  def score(text: String, topK: Int): Seq[ScoredPair] = {
    val concept = ConceptUtils.conceptBOWFromString(text, word2vec, flat = true)
    val scoredPairs = aligner.topk(concept, conceptSequence, topK)

    scoredPairs
  }
}
