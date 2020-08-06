package org.clulab.alignment.data.datamart

class DatamartTokenizer {

  def tokenize(text: String): Array[String] = {
    text
        .split(' ')
        .filter(!_.isEmpty)
        .map(_.toLowerCase)
  }
}

object DatamartTokenizer {

  def apply(): DatamartTokenizer = new DatamartTokenizer()
}
