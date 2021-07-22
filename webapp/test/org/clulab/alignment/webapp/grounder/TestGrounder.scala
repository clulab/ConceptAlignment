package org.clulab.alignment.webapp.grounder

import org.clulab.alignment.Test
import org.clulab.alignment.utils.FileUtils

import java.io.File
import scala.collection.mutable


class TestGrounder extends Test {

  def findFilename(filename: String): String = {
    if (!new File(filename).exists) "." + filename
    else filename
  }

  val modelFilename = findFilename("./doc/model.example.json")
  val indicatorFilename = findFilename("./doc/indicator.example.json")

  val fullModelFilename = findFilename("./doc/model.full.example.json")

  behavior of "Grounder"

  it should "read a model document" in {
    val model = FileUtils.getTextFromFile(modelFilename)

    new ModelDocument(model)
  }

  it should "read an indicator document" in {
    val indicator = FileUtils.getTextFromFile(indicatorFilename)

    new IndicatorDocument(indicator)
  }

  it should "handle tags in a document" in {

// Then in a variable
    // For document, parameters, outputs, qualifier_outputs
    val model = FileUtils.getTextFromFile(fullModelFilename)

    {
      // Specified and full, which is the default
      val modelDocument = new ModelDocument(model)
      modelDocument.tags.length should be (4)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("tags") = ujson.Arr() // Specified and empty
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.tags.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("tags") = ujson.Null // Specified and null
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.tags.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj.remove("tags") // Not specified
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.tags.length should be(0)
    }
  }
}
