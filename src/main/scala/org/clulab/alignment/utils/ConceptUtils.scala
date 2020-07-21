package org.clulab.alignment.utils

import com.typesafe.config.Config
import ai.lum.common.ConfigUtils._

import org.clulab.alignment.{CompositionalConcept, Concept, ConceptSequence, FlatConcept}
import org.clulab.embeddings.word2vec.Word2Vec
import org.clulab.wm.eidos.{EidosProcessor, EidosSystem}
import org.clulab.wm.eidos.groundings.{EidosOntologyGrounder, OntologyHandler}
import org.clulab.wm.eidos.utils.StopwordManager

object ConceptUtils {

  // TODO: ask Becky, why my program could not find this fromConfig function?
  // lazy val ontologyHandler = OntologyHandler.fromConfig()
  lazy val config = EidosSystem.defaultConfig // TODO: ask becky, it only works using lum.ai 0.0.7
  lazy val processor = EidosProcessor("english", cutoff = 150)
  lazy val tagSet = processor.getTagSet
  lazy val ontologyHandler = OntologyHandler.load(
    config[Config]("ontologies"),
    processor,
    StopwordManager.fromConfig(config, tagSet),
    tagSet
  )

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
