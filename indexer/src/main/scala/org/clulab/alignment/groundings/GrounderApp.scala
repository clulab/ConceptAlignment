package org.clulab.alignment.groundings

import org.clulab.alignment.aligner.EmbeddingOnlyAligner
import org.clulab.alignment.groundings.grounders.DatamartGrounder
import org.clulab.alignment.groundings.ontologies.DatamartOntology
import org.clulab.embeddings.word2vec.CompactWord2Vec

object GrounderApp extends App {
  val filename = "./src/main/resources/datamarts.tsv"
  val parser = new DatamartParser()
  val ontology = DatamartOntology.fromFile(filename, parser)
  val aligner = new EmbeddingOnlyAligner()
  val word2vec = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", true, false)
  val grounder = new DatamartGrounder(ontology, word2vec, aligner)
  val scoredPairs = grounder.score("water shortages make people thirsty", 5)

  scoredPairs.foreach { scoredPair => println(scoredPair.src + "\t" + scoredPair.value) }
}
