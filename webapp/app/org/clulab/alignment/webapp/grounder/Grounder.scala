package org.clulab.alignment.webapp.grounder

import org.clulab.alignment.data.Tokenizer

trait Grounding {
  def toJson: String
}

class FlatGrounding(name: String, score: Float) extends Grounding {
  def toJson: String = ""
}

class CompGrounding extends Grounding {
  def toJson: String = ""
}

trait Groundable {
  def groundFlat(): FlatGrounding
  def groundComp(): CompGrounding
}

class DojoVariable(jVal: ujson.Value, dojoDocument: DojoDocument) extends Groundable {
  val jObj = jVal.obj
  val name = jObj("name").str // required
  val displayName = jObj("display_name").str // required
  val description = jObj("description").str // required
  val tags = jObj.get("tags").map(_.arr.toArray.map(_.str)).getOrElse(Array.empty[String]) // optional
  val words = {
    val tokenizer = Tokenizer()
    val displayNameWords = tokenizer.tokenize(displayName)
    val descriptionWords = tokenizer.tokenize(description)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    displayNameWords ++ descriptionWords ++ tagWords ++ dojoDocument.getWords
  }

  def groundFlat(): FlatGrounding = ??? // Need ontology indexes

  def groundComp(): CompGrounding = ???
}

class DojoParameter(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

abstract class DojoDocument(json: String) {
  val jObj = ujson.read(json).obj
  val id = jObj("id").str // required
  // name // required // doesn't look helpful
  val description = jObj("description").str // required
  val categories = jObj("category").arr.map(_.str) // required
  val tags = jObj.get("tags").map(_.arr.toArray.map(_.str)).getOrElse(Array.empty[String]) // optional
  //  geography // optional // These won't be in our ontologies anyway.
  val words = {
    val tokenizer = Tokenizer()
    val descriptionWords = tokenizer.tokenize(description)
    val categoryWords = categories.flatMap(tokenizer.tokenize)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    descriptionWords ++ categoryWords ++ tagWords
  }

  def getWords: Array[String] = words
}

trait GroundedDocument {
  def toJson: String
}

class GroundedModelDocument(modelDocument: ModelDocument, parameterGroundings: Seq[Grounding], outputGroundings: Seq[Grounding]) extends GroundedDocument {
  def toJson: String = ""
}

class ModelDocument(json: String) extends DojoDocument(json) {
  val parameters = jObj("parameters").arr.map(new DojoParameter(_, this)) // required
  val outputs = jObj("outputs").arr.map(new DojoOutput(_, this)) // required

  def groundFlat(): GroundedModelDocument = {
    val parameterGrounds = parameters.map(_.groundFlat)
    val outputGrounds = outputs.map(_.groundFlat)
    new GroundedModelDocument(this, parameterGrounds, outputGrounds)
  }

  def groundComp(): GroundedModelDocument = {
    val parameterGrounds = parameters.map(_.groundComp)
    val outputGrounds = outputs.map(_.groundComp)
    new GroundedModelDocument(this, parameterGrounds, outputGrounds)
  }
}

class GroundedIndicatorDocument(indicatorDocument: IndicatorDocument, outputGrounds: Seq[Grounding]) extends GroundedDocument {
  def toJson: String = ""
}

class IndicatorDocument(json: String) extends DojoDocument(json) {
  val outputs = jObj("outputs").arr.map(new DojoOutput(_, this)) // required

  def groundFlat(): GroundedIndicatorDocument = {
    val outputGrounds = outputs.map(_.groundFlat)
    new GroundedIndicatorDocument(this, outputGrounds)
  }

  def groundComp(): GroundedIndicatorDocument = {
    val outputGrounds = outputs.map(_.groundComp)
    new GroundedIndicatorDocument(this, outputGrounds)
  }
}
