package org.clulab.alignment.experiment

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.grounder.datamart.DatamartOntology
import org.clulab.alignment.utils.OntologyHandlerHelper
import org.clulab.wm.eidos.EidosSystem
import org.clulab.wm.eidos.groundings.ConceptEmbedding
import org.clulab.wm.eidos.groundings.EidosOntologyGrounder
import org.clulab.wm.eidos.groundings.OntologyHandler

import scala.collection.JavaConverters._

object ExperimentApp extends App {

  def getConcepts(ontologyHandler: OntologyHandler, namespace: String): Seq[ConceptEmbedding] = {
    val eidosOntologyGrounder = ontologyHandler.ontologyGrounders
        .collect { case grounder: EidosOntologyGrounder => grounder}
        .find { grounder => grounder.name == namespace }
        .get

    eidosOntologyGrounder.conceptEmbeddings
  }

  val config = ConfigFactory
      .empty
      .withValue("ontologies.ontologies", ConfigValueFactory.fromIterable(
        // Both of these are needed and Eidos isn't configured that way by default.
        Seq("wm_flattened").asJava // , "wm_compositional").asJava // coming soon
      ))
      .withFallback(EidosSystem.defaultConfig)
  // This happens to have the embeddings along with the ontologies.
  // It will take a long time, unfortunately.
  val ontologyHandler = OntologyHandlerHelper.fromConfig()
  // One oversimplified way to align is to use these embeddings.
  val word2vec = ontologyHandler.wordToVec

  // Bottom-up things
  val filename = "./experiment/src/main/resources/datamarts.tsv"
  val parser = new Tokenizer()
  val bottomUpEntries = DatamartOntology.fromFile(filename, parser).datamartEntries

  bottomUpEntries.foreach { datamartEntry =>
    // These are just collections of words so far.  More structure coming shortly.
    val embedding = word2vec.makeCompositeVector(datamartEntry.words).take(10).mkString(" ")
    println(s"${datamartEntry.identifier} => $embedding")
  }

  // Top-down things
  val flatConceptEmbeddings = getConcepts(ontologyHandler, "wm_flattened")
  val compositionalConceptEmbeddings = Seq.empty // getConcepts(ontologyHandler, "wm_compositional") // coming soon

  (flatConceptEmbeddings ++ compositionalConceptEmbeddings).foreach { conceptEmbedding =>
    // These happen to come with embeddings already.
    val embedding = conceptEmbedding.embedding.take(10).mkString(" ")
    println(s"${conceptEmbedding.namer.name} $embedding")
  }
}
