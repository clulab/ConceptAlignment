package org.clulab.alignment.webapp

import org.clulab.alignment.Test

class TestFileUtils extends Test {

  behavior of "FileUtils"

  it should "findFileAndIndex" in {
    val result = FileUtils.findFileAndIndex("..", "index_")

    println(result)
  }

  ignore should "delete" in {
    FileUtils.rmdir("../index_2")
  }
}
