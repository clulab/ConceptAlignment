package org.clulab.alignment.experiment

import org.clulab.alignment.Test

class TestExperiment extends Test {

  behavior of "Experiment"

  it should "produce marvelous results" in {
    val experiment = true

    experiment should be (true)
  }
}
