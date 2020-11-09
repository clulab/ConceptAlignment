package org.clulab.alignment.grounder.datamart

import org.clulab.alignment.ConceptSequence
import org.clulab.alignment.aligner.Aligner
import org.clulab.alignment.aligner.ScoredPair
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.data.datamart.DatamartConcept
import org.clulab.alignment.utils.ConceptUtils
import org.clulab.embeddings.word2vec.CompactWord2Vec

class DatamartGrounder(ontology: DatamartOntology, word2vec: CompactWord2Vec, aligner: Aligner) {
  val tokenizer: Tokenizer = Tokenizer()
  val datamartConcepts: Seq[DatamartConcept] = ontology.datamartEntries.map { datamartEntry =>
    val identifier = datamartEntry.identifier
    val words = datamartEntry.words
    // TODO: This might need to be done differently.
    val embedding = word2vec.makeCompositeVector(words)

    DatamartConcept(identifier, embedding)
  }
  val conceptSequence: ConceptSequence = ConceptSequence(datamartConcepts)

  def score(text: String, topK: Int): Seq[ScoredPair] = {
    val concept = ConceptUtils.conceptBOWFromString(text, word2vec, flat = true)
    val scoredPairs = aligner.topk(concept, conceptSequence, topK)

    scoredPairs
  }
}
