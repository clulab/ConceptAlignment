package org.clulab.alignment.groundings

import org.clulab.alignment.groundings.grounders.DatamartGrounder
import org.clulab.alignment.groundings.ontologies.DatamartOntology
import org.clulab.alignment.scraper.IsiScraper
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.EidosWordToVec
import org.clulab.wm.eidos.groundings.RealWordToVec

object GrounderApp extends App {
  val grounder = {
    val filename = "./src/main/resources/datamarts.tsv"
    val maxTopK = 10
    val word2vec: EidosWordToVec = {
      val w2v = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
      new RealWordToVec(w2v, maxTopK)
    }
    val parser = new DatamartParser()
    val ontology = DatamartOntology.fromFile(filename, word2vec, parser)

    new DatamartGrounder(parser, ontology, word2vec)
  }
  val groundings = grounder.ground("water shortages make people thirsty", 5)

  groundings.foreach { grounding => println(grounding.namer.name + "\t" + grounding.value) }
}
