package org.clulab.alignment.data

class Tokenizer {

  def tokenize(text: String): Array[String] = text
      .filterNot(Tokenizer.punctuation)
      .toLowerCase
      .split(' ')
      .filterNot(_.isEmpty)
}

object Tokenizer {
  val punctuation: Set[Char] = ".!?,;:/*(){}[]".toSet

  def apply(): Tokenizer = new Tokenizer()
}
