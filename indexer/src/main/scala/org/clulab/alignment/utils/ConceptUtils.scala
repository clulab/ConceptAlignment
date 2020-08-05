package org.clulab.alignment.utils

import org.clulab.alignment.{CompositionalConcept, Concept, ConceptSequence, FlatConcept}
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.{EidosOntologyGrounder, OntologyHandler}

object ConceptUtils {

  // This has to wait for Eidos 1.1.0.
  lazy val ontologyHandler: OntologyHandler = null // OntologyHandler.fromConfig()

  def conceptBOWFromString(s: String, w2v: CompactWord2Vec, flat: Boolean): Concept = {
    val tokens = s.split(" ").map(_.trim.toLowerCase())
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
