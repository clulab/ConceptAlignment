package org.clulab.alignment

class TestSingleKnnApp extends Test {
  val singleKnnApp = new SingleKnnApp()

  behavior of "SingleKnnApp"

  it should "not crash with results" in {
    val datamartDocumentsAndScores = singleKnnApp.run("food", 10, None)
  }

  it should "not crash if without results" in {
    val datamartDocumentsAndScores = singleKnnApp.run("Oromia", 10, None)
  }

  def countHits(fromTerm: String, toTerm: String): Int = {
    val datamartDocumentsAndScores = singleKnnApp.run(fromTerm, 100, None)
    val hits = datamartDocumentsAndScores.count { case (datamartDocument, _) =>
      datamartDocument.variableName.toLowerCase.contains(toTerm) ||
          datamartDocument.variableDescription.toLowerCase.contains(toTerm)
    }
    hits
  }

  def testCrossReference(leftTerm: String, rightTerm: String): Unit = {
    countHits(leftTerm, rightTerm) should be > 0
    countHits(rightTerm, leftTerm) should be > 0
  }

  // This test is dependent on datamart entries which are in flux.
  ignore should "cross reference corn and maize" in {
    testCrossReference("corn", "maize")
  }

  it should "divulge all results" in {
    val datamartSize = singleKnnApp.datamartIndex

  }
}
