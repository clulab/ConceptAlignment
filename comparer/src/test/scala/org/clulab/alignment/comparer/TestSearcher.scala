package org.clulab.alignment.comparer

import org.clulab.alignment.Test

class TestSearcher extends Test {

  behavior of "Searcher"

  it should "work as well as or better than before" in {
    val (oldScoreTotal, newScoreTotal) = new ExperimentSpreadsheetsApp().run()

    oldScoreTotal should be <= newScoreTotal
  }
}
