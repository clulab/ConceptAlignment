package org.clulab.alignment

class TestSingleKnnApp extends Test {
  val singleKnnApp = new SingleKnnApp()

  behavior of "SingleKnnApp"

  it should "not crash with results" in {
    val datamartDocumentsAndScores = singleKnnApp.run("food", 10)
  }

  it should "not crash if without results" in {
    val datamartDocumentsAndScores = singleKnnApp.run("Oromia", 10)
  }
}
