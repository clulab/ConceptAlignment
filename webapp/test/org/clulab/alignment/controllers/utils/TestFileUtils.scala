package org.clulab.alignment.controllers.utils

import org.clulab.alignment.Test

class TestFileUtils extends Test {

  behavior of "findIndex"

  it should "work" in {
    val result = FileUtils.findIndex("..", "index_")

  }
}
