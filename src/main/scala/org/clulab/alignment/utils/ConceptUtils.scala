package org.clulab.alignment.utils

import org.clulab.alignment.{CompositionalConcept, Concept, ConceptSequence, FlatConcept}
import org.clulab.embeddings.word2vec.Word2Vec
import org.clulab.wm.eidos.groundings.{EidosOntologyGrounder, OntologyHandler}

object ConceptUtils {

  lazy val ontologyHandler = OntologyHandler.fromConfig()

  def conceptBOWFromString(s: String, w2v: Word2Vec, flat: Boolean): Concept = {
    val tokens = s.split(" ").map(_.trim.toLowerCase())
    val emb = w2v.makeCompositeVector(tokens).map(_.toFloat)
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
