package org.clulab.alignment.webapp.grounder

import org.clulab.alignment.Test
import org.clulab.alignment.utils.FileUtils

import java.io.File

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

  it should "handle tags in a variable" in {
    val model = FileUtils.getTextFromFile(fullModelFilename)

    {
      // Specified and full, which is the default
      val modelDocument = new ModelDocument(model)
      modelDocument.parameters.head.tags.length should be (3)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("parameters").arr.head("tags") = ujson.Arr() // Specified and empty
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.parameters.head.tags.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("parameters").arr.head("tags") = ujson.Null // Specified and null
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.parameters.head.tags.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("parameters").arr.head.obj.remove("tags") // Not specified
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.parameters.head.tags.length should be(0)
    }
  }

  it should "handle categories in a document" in {
    val model = FileUtils.getTextFromFile(fullModelFilename)

    {
      // Specified and full, which is the default
      val modelDocument = new ModelDocument(model)
      modelDocument.categories.length should be (2)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("category") = ujson.Arr() // Specified and empty
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.categories.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("category") = ujson.Null // Specified and null
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.categories.length should be(0)
    }
  }

  it should "handle qualifier outputs in a document" in {
    val model = FileUtils.getTextFromFile(fullModelFilename)

    {
      // Specified and full, which is the default
      val modelDocument = new ModelDocument(model)
      modelDocument.qualifierOutputsOpt.get.length should be (1)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("qualifier_outputs") = ujson.Arr() // Specified and empty
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.qualifierOutputsOpt.get.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj("qualifier_outputs") = ujson.Null // Specified and null
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.qualifierOutputsOpt.get.length should be(0)
    }

    {
      val newModel = {
        val jObj = ujson.read(model).obj
        jObj.remove("qualifier_outputs") // Not specified
        val newModel = ujson.Obj(jObj).render(4)
        newModel
      }

      val newModelDocument = new ModelDocument(newModel)
      newModelDocument.qualifierOutputsOpt should be(empty)
    }
  }
}
