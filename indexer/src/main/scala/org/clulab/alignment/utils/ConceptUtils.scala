package org.clulab.alignment.utils

import org.clulab.alignment.{CompositionalConcept, Concept, ConceptSequence, FlatConcept}
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.RealWordToVec
import org.clulab.wm.eidos.groundings.{EidosOntologyGrounder, OntologyHandler}

object ConceptUtils {
  lazy val ontologyHandler: OntologyHandler = OntologyHandlerHelper.fromConfig()
  lazy val word2Vec: CompactWord2Vec = ontologyHandler.wordToVec.asInstanceOf[RealWordToVec].w2v

  def conceptBOWFromString(s: String, w2v: CompactWord2Vec, flat: Boolean): Concept = {
    val tokens = s.split(' ').map(_.trim).filter(_.nonEmpty).map(_.toLowerCase())
    val emb = w2v.makeCompositeVector(tokens)
    val flatConcept = new FlatConcept(s, emb)
    if (flat) {
      flatConcept
    } else {
      new CompositionalConcept(s, flatConcept, Seq())
    }
  }

  def conceptsFromWMOntology(namespace: String): ConceptSequence = {
    val TDOntology = ontologyHandler.ontologyGrounders
      .collect { case grounder: EidosOntologyGrounder => grounder}
      .find { grounder => grounder.name == namespace }.get
    val TDConceptEmbeddings = TDOntology.conceptEmbeddings
    ConceptSequence(TDConceptEmbeddings.map(ce => new FlatConcept(ce.namer.name, ce.embedding)))
  }

}
