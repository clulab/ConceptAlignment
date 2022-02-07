package org.clulab.alignment.comparer

import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, Sourcer, TsvReader, TsvWriter}
import org.clulab.alignment.webapp.searcher.{Searcher, SearcherLocations}
import org.clulab.wm.eidoscommon.utils.StringUtils

/**
 * This was supposed to add scores to the spreadsheet.
 * */
object NumberSpreadsheetsApp extends App {
  val ataInputFilename = "../comparer/04-ATA.tsv"
  val nafInputFilename = "../comparer/04-NAF.tsv"
  val xtraInputFilename = "../comparer/04-xtra.tsv"
  val inputFilenames = Seq(ataInputFilename, nafInputFilename, xtraInputFilename)

  val ataOutputFilename = "../comparer/04-ATA-out.tsv"
  val nafOutputFilename = "../comparer/04-NAF-out.tsv"
  val xtraOutputFilename = "../comparer/04-xtra-out.tsv"
  val outputFilenames = Seq(ataOutputFilename, nafOutputFilename, xtraOutputFilename)

  val maxHits = 10
  val thresholdOpt = None
  val tsvReader = new TsvReader()

  val searcherLocations = new SearcherLocations(1, "../builder")
  val searcher = new Searcher(searcherLocations)

  while (!searcher.isReady)
    Thread.sleep(100)

  def mkHomeAndAwayIds(strings: Array[String]): Option[(CompositionalOntologyIdentifier, Option[CompositionalOntologyIdentifier])] = {
    def getSlot(string: String): String = StringUtils.beforeFirst(StringUtils.afterFirst(string, '/'), '/')

    def newId(string: String) = FlatOntologyIdentifier("wm_compositional", string, Some(getSlot(string)))

    def getId(slots: Array[String], strings: Array[String]): CompositionalOntologyIdentifier = {
      slots match {
        case Array("concept") =>
          CompositionalOntologyIdentifier(newId(strings(0)), None, None, None)
        case Array("concept", "property") =>
          CompositionalOntologyIdentifier(newId(strings(0)), Some(newId(strings(1))), None, None)
        case Array("concept", "property", "process") =>
          CompositionalOntologyIdentifier(newId(strings(0)), Some(newId(strings(1))), Some(newId(strings(2))), None)
        case Array("concept",             "process") =>
          CompositionalOntologyIdentifier(newId(strings(0)), None, Some(newId(strings(1))), None)
        case Array("concept",             "process", "property") =>
          CompositionalOntologyIdentifier(newId(strings(0)), None, Some(newId(strings(1))), Some(newId(strings(2))))
      }
    }

    val slots = strings.map(getSlot)
    val count = slots.count(_ == "concept")

    count match {
      case 0 => None
      case 1 => Some(getId(slots, strings), None)
      case 2 =>
        val split = slots.indexOf("concept", 1) // Skip the first concept.
        Some(getId(slots.take(split), strings.take(split)), Some(getId(slots.drop(split), strings.drop(split))))
      case _ => ???
    }
  }

  inputFilenames.zipWithIndex.foreach { case (inputFilename, index) =>
    Sourcer.sourceFromFile(inputFilename).autoClose { source =>
      val lines = source.getLines

      FileUtils.printWriterFromFile(outputFilenames(index)).autoClose { printWriter =>
        val xsvWriter = new TsvWriter(printWriter)
        val header = lines.next()

        xsvWriter.println("Concept name", "OntologyNodes", "Assigned indicator", "Default indicator match",
          "DatamartIdentifier1", "DatamartIdentifier2", "DatamartIdentifier3",
          "DatamartIdentifier1a", "DatamartIdentifier2a", "DatamartIdentifier3a",
          "Score1", "Score2", "Score3"
        )

        lines.foreach { line =>
          val Array(
            conceptName,
            ontologyNodes,
            assignedIndicator,
            defaultIndicatorMatch,
            datamartIdentifier1,
            datamartIdentifier2,
            datamartIdentifier3,
          ) = tsvReader.readln(line, 7)
          val homeIdAndAwayIdOptOpt = mkHomeAndAwayIds(ontologyNodes.split(' '))

          if (homeIdAndAwayIdOptOpt.isDefined) {
            val (homeId, awayIdOpt) = homeIdAndAwayIdOptOpt.get
            val datamartIdentifiersAndValues = searcher.runOld(homeId, awayIdOpt.toArray, maxHits, thresholdOpt).dstResults.take(3)
            val (datamartIdentifiers, floats) = datamartIdentifiersAndValues.unzip
            val values = floats.map(_.toString)

            xsvWriter.println(Seq(
              conceptName, ontologyNodes, assignedIndicator, defaultIndicatorMatch,
              datamartIdentifier1, datamartIdentifier2, datamartIdentifier3,
            ) ++ datamartIdentifiers ++ values)
          }
          else {
            // Search on the string instead?
            xsvWriter.println(Seq(
              conceptName, ontologyNodes, assignedIndicator, defaultIndicatorMatch,
              datamartIdentifier1, datamartIdentifier2, datamartIdentifier3,
              "", "", "", "", "", ""
            ))
          }
        }
      }
    }
  }
}
