package org.clulab.alignment.groundings

class DatamartParser {

  def parse(text: String): Array[String] = {
    text.split(' ')
  }
}

object DatamartParser {

  def apply(): DatamartParser = new DatamartParser()
}
