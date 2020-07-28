package org.clulab.alignment

import org.clulab.alignment.utils.ConceptUtils

object Example extends App {

  // Get the WM ontology(ies) being used and convert to the local data structure
  val (tdConcepts, w2v) = ConceptUtils.conceptsFromWMOntology("wm_flattened")

  val aligner = WeightedParentSimilarityAligner.fromConfig(w2v)

  val indicatorExamples = Seq(
    "Annual growth US$ Gross Domestic Product per capita",
    "Area Agricultural area organic, total",
    "Area harvested Maize",
  ).map(ConceptUtils.conceptBOWFromString(_, aligner.w2v, flat = true))

  val top5 = indicatorExamples.flatMap(indicator => aligner.topk(indicator, tdConcepts, 5))
  top5 foreach println

}
