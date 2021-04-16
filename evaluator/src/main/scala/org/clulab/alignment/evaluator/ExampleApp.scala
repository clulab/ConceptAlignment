package org.clulab.alignment.evaluator

import org.clulab.alignment.aligner.WeightedParentSimilarityAligner
import org.clulab.alignment.utils.ConceptUtils

object ExampleApp extends App {
  // Get the WM ontology(ies) being used and convert to the local data structure
  val tdConcepts = ConceptUtils.conceptsFromWMOntology("wm_flattened")
  val w2v = ConceptUtils.word2Vec
  val aligner = WeightedParentSimilarityAligner.fromConfig(w2v)

  val indicatorExamples = Seq(
    "Annual growth US$ Gross Domestic Product per capita",
    "Area Agricultural area organic, total",
    "Area harvested Maize",
  ).map(ConceptUtils.conceptBOWFromString(_, aligner.w2v, flat = true))

  val top5 = indicatorExamples.flatMap(indicator => aligner.topk(indicator, tdConcepts, 5))
  top5 foreach println
}
