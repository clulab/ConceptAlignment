package org.clulab.alignment.comparer

import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, Sourcer}
import org.clulab.alignment.webapp.grounder.IndicatorDocument
import org.clulab.alignment.webapp.searcher.{Searcher, SearcherLocations}
import ujson.Value

object GrounderApp extends App {
  val inputFilename = "../comparer/indicators_11082021.jsonl"
  val outputFilename = "../comparer/grounded_indicators_11082021.jsonl"

  val maxHits = 10
  val thresholdOpt = Some(0.6f)
  val compositional = true

  val searcherLocations = new SearcherLocations(1, "../builder")
  val searcher = new Searcher(searcherLocations)

  while (!searcher.isReady)
    Thread.sleep(100)

  def sizeOf(seqOpt: Option[Seq[_]]): Int = seqOpt.map(_.size).getOrElse(0)

  def optionOf(jValue: Value): Option[Value] =
      if (jValue.isNull) None
      else Some(jValue)

  FileUtils.printWriterFromFile(outputFilename).autoClose { printWriter =>
    Sourcer.sourceFromFile(inputFilename).autoClose { source =>
      source.getLines.foreach { line =>
        val dojoDocument = new IndicatorDocument(line)
        val inputJson = searcher.run(dojoDocument, maxHits, thresholdOpt, compositional)

        try {
          val oldJValue = ujson.read(line).obj
          val newJValue = ujson.read(inputJson).obj

          val oldOutputsOpt = Option(oldJValue("outputs")).map(_.arr)
          val newOutputsOpt = Option(newJValue("outputs")).map(_.arr)
          val oldOutputsSize = sizeOf(oldOutputsOpt)
          val newOutputsSize = sizeOf(newOutputsOpt)

          val oldQualifierOutputsOpt = optionOf(oldJValue("qualifier_outputs")).map(_.arr)
          val newQualifierOutputsOpt = optionOf(newJValue("qualifier_outputs")).map(_.arr)
          val oldQualifierOutputsSize = sizeOf(oldQualifierOutputsOpt)
          val newQualifierOutputsSize = sizeOf(newQualifierOutputsOpt)

          if (oldOutputsSize != newOutputsSize)
            throw new RuntimeException("Output sizes are mismatched.")
          if (oldQualifierOutputsSize != newQualifierOutputsSize)
            throw new RuntimeException("Qualifier output sizes are mismatched.")

          if (oldOutputsSize > 0) {
            val oldOutputs = oldOutputsOpt.get
            val newOutputs = newOutputsOpt.get

            oldOutputs.zip(newOutputs).foreach { case (oldOutput, newOutput) =>
              val oldName = oldOutput("name").str
              val newName = newOutput("name").str

              if (oldName != newName)
                throw new RuntimeException("Names do not match up.")
              oldOutput("ontologies") = newOutput("ontologies")
            }
          }

          if (oldQualifierOutputsSize > 0) {
            val oldQualifierOutputs = oldQualifierOutputsOpt.get
            val newQualifierOutputs = newQualifierOutputsOpt.get

            oldQualifierOutputs.zip(newQualifierOutputs).foreach { case (oldQualifierOutput, newQualifierOutput) =>
              val oldName = oldQualifierOutput("name").str
              val newName = newQualifierOutput("name").str

              if (oldName != newName)
                throw new RuntimeException("Qualifier names do not match up.")
              oldQualifierOutput("ontologies") = newQualifierOutput("ontologies")
            }
          }

          val outputJson = oldJValue.render(-1, escapeUnicode = true).replace('\n', ' ')
          println(outputJson)
          printWriter.println(outputJson)
        }
        catch {
          case runtimeException: RuntimeException =>
            runtimeException.printStackTrace()
        }
      }
    }
  }
}
