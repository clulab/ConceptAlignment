package org.clulab.alignment.webapp.grounder

import org.clulab.alignment.Test
import org.clulab.alignment.utils.FileUtils

class TestGrounder extends Test {

  behavior of "Grounder"

  it should "read a model document" in {
    val model = FileUtils.getTextFromFile("../model.example.json")
    val modelDocument = new ModelDocument(model)
    val groundedModelDocumentFlat = modelDocument.groundFlat()
    val groundedModelDocumentComp = modelDocument.groundComp()

    println(groundedModelDocumentFlat.toJson)
    println(groundedModelDocumentComp.toJson)
  }

  it should "read an indicator document" in {
    val indicator = FileUtils.getTextFromFile("../indicator.example.json")
    val indicatorDocument = new IndicatorDocument(indicator)
    val groundedIndicatorDocumentFlat = indicatorDocument.groundFlat()
    val groundedIndicatorDocumentComp = indicatorDocument.groundComp()

    println(groundedIndicatorDocumentFlat.toJson)
    println(groundedIndicatorDocumentComp.toJson)
  }
}
