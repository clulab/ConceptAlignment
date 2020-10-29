package org.clulab.alignment.webapp

import org.clulab.alignment.Test

class TestFileUtils extends Test {

  behavior of "findIndex"

  it should "work" in {
    val result = FileUtils.findFileAndIndex("..", "index_")

    println(result)
  }
}
