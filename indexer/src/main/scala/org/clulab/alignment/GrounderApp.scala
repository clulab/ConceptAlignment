package org.clulab.alignment

import org.clulab.alignment.aligner.EmbeddingOnlyAligner
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.grounder.datamart.DatamartGrounder
import org.clulab.alignment.grounder.datamart.DatamartOntology
import org.clulab.embeddings.word2vec.CompactWord2Vec

object GrounderApp extends App {
  val filename = "./src/main/resources/datamarts.tsv"
  val parser = new Tokenizer()
  val ontology = DatamartOntology.fromFile(filename, parser)
  val aligner = new EmbeddingOnlyAligner()
  val word2vec = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
  val grounder = new DatamartGrounder(ontology, word2vec, aligner)
  val scoredPairs = grounder.score("water shortages make people thirsty", 5)

  scoredPairs.foreach { scoredPair => println(scoredPair.src + "\t" + scoredPair.value) }
}
