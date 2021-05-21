package org.clulab.alignment.webapp.grounder

import org.clulab.alignment.data.Tokenizer

import scala.collection.mutable

class SingleGrounding(name: String, score: Float) {
  def toJson: ujson.Value = ujson.Obj(
    ("name", name),
    ("score", score)
  )
}

trait Groundings {
  def toJVal: ujson.Value
}

class FlatGroundings(groundings: Seq[SingleGrounding]) extends Groundings {

  def toJVal: ujson.Value = ujson.Arr(
    groundings.map(_.toJson):_*
  )
}

class CompGroundings(concepts: FlatGroundings, processes: FlatGroundings, properties: FlatGroundings) extends Groundings {
  def toJVal: ujson.Value = ujson.Obj(
    ("concepts", concepts.toJVal),
    ("processes", processes.toJVal),
    ("properties", properties.toJVal)
  )
}

trait Groundable {
  def groundFlat(): FlatGroundings
  def groundComp(): CompGroundings
}

class DojoVariable(jVal: ujson.Value, dojoDocument: DojoDocument) extends Groundable {
  protected val jObj: mutable.Map[String, ujson.Value] = jVal.obj
  val name: String = jObj("name").str // required
  val displayName: String = jObj("display_name").str // required
  val description: String = jObj("description").str // required
  val tags: Array[String] = jObj.get("tags").map(_.arr.toArray.map(_.str)).getOrElse(Array.empty[String]) // optional
  val words: Array[String] = {
    val tokenizer = Tokenizer()
    val displayNameWords = tokenizer.tokenize(displayName)
    val descriptionWords = tokenizer.tokenize(description)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    displayNameWords ++ descriptionWords ++ tagWords ++ dojoDocument.getWords
  }

  def groundFlat(): FlatGroundings = {
    new FlatGroundings(Seq(
      new SingleGrounding("wm/entity/people/", 0.6891140341758728f),
      new SingleGrounding("wm/research", 0.6033900380134583f),
      new SingleGrounding("wm/condition", 0.6441149115562439f)
    ))
  }

  def groundComp(): CompGroundings = {
    new CompGroundings(
      new FlatGroundings(Seq(
        new SingleGrounding("wm/concept/entity/people/", 0.6891140341758728f),
        new SingleGrounding("wm/concept/humanitarian_assistance/food_aid", 0.6679953932762146f)
      )),
      new FlatGroundings(Seq(
        new SingleGrounding("wm/process/research", 0.6033900380134583f),
        new SingleGrounding("wm/process/train/agriculture_training", 0.5917248129844666f)
      )),
      new FlatGroundings(Seq(
        new SingleGrounding("wm/property/condition", 0.6441149115562439f),
        new SingleGrounding("wm/property/preference", 0.5431265830993652f)
      ))
    )
  }
}

class DojoParameter(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

class DojoOutput(jVal: ujson.Value, dojoDocument: DojoDocument) extends DojoVariable(jVal, dojoDocument)

abstract class DojoDocument(json: String) {
  protected val jObj: mutable.Map[String, ujson.Value] = ujson.read(json).obj
  val id: String = jObj("id").str // required
  // name // required // doesn't look helpful
  val description: String = jObj("description").str // required
  val categories: Array[String] = jObj("category").arr.toArray.map(_.str) // required
  val tags: Array[String] = jObj.get("tags").map(_.arr.toArray.map(_.str)).getOrElse(Array.empty[String]) // optional
  //  geography // optional // These won't be in our ontologies anyway.
  val words: Array[String] = {
    val tokenizer = Tokenizer()
    val descriptionWords = tokenizer.tokenize(description)
    val categoryWords = categories.flatMap(tokenizer.tokenize)
    val tagWords = tags.flatMap(tokenizer.tokenize)

    descriptionWords ++ categoryWords ++ tagWords
  }

  def getWords: Array[String] = words
}

abstract class GroundedDocument {

  protected def toJVal(variable: DojoVariable, groundings: Groundings): ujson.Obj = {
    ujson.Obj(
      ("name", variable.name),
      ("ontologies", groundings.toJVal)
    )
  }

  protected def toJVal(variables: Seq[DojoVariable], multipleGroundings: Seq[Groundings]): ujson.Arr = {
    ujson.Arr(
      variables.zip(multipleGroundings).map { case (variable, groundings) =>
        toJVal(variable, groundings)
      }:_*
    )
  }

  def toJVal: ujson.Value

  def toJson: String = toJVal.render(4)
}

class GroundedModelDocument(modelDocument: ModelDocument, parameterGroundings: Seq[Groundings], outputGroundings: Seq[Groundings]) extends GroundedDocument {

  def toJVal: ujson.Value = {
    ujson.Obj(
      ("id", modelDocument.id),
      ("parameters", toJVal(modelDocument.parameters, parameterGroundings)),
      ("outputs", toJVal(modelDocument.outputs, outputGroundings))
    )
  }
}

class ModelDocument(json: String) extends DojoDocument(json) {
  val parameters: Array[DojoParameter] = jObj("parameters").arr.toArray.map(new DojoParameter(_, this)) // required
  val outputs: Array[DojoOutput] = jObj("outputs").arr.toArray.map(new DojoOutput(_, this)) // required

  def groundFlat(): GroundedModelDocument = {
    val parameterGrounds = parameters.map(_.groundFlat())
    val outputGrounds = outputs.map(_.groundFlat())
    new GroundedModelDocument(this, parameterGrounds, outputGrounds)
  }

  def groundComp(): GroundedModelDocument = {
    val parameterGrounds = parameters.map(_.groundComp())
    val outputGrounds = outputs.map(_.groundComp())
    new GroundedModelDocument(this, parameterGrounds, outputGrounds)
  }
}

class GroundedIndicatorDocument(indicatorDocument: IndicatorDocument, outputGroundings: Seq[Groundings]) extends GroundedDocument {

  def toJVal: ujson.Value = {
    ujson.Obj(
      ("id", indicatorDocument.id),
      ("outputs", toJVal(indicatorDocument.outputs, outputGroundings))
    )
  }
}

class IndicatorDocument(json: String) extends DojoDocument(json) {
  val outputs: Array[DojoOutput] = jObj("outputs").arr.toArray.map(new DojoOutput(_, this)) // required

  def groundFlat(): GroundedIndicatorDocument = {
    val outputGrounds = outputs.map(_.groundFlat())
    new GroundedIndicatorDocument(this, outputGrounds)
  }

  def groundComp(): GroundedIndicatorDocument = {
    val outputGrounds = outputs.map(_.groundComp())
    new GroundedIndicatorDocument(this, outputGrounds)
  }
}
