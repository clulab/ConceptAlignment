package org.clulab.alignment.comparer

import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibDatamartIndexerApp
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex.Index
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.{FileUtils, Sourcer, TsvReader, TsvWriter}
import org.clulab.alignment.webapp.searcher.{Searcher, SearcherLocations}
import org.clulab.wm.eidoscommon.utils.StringUtils

import java.io.File

// Which of our answers were in the gold?
class ExperimentSpreadsheetsApp() {
  val baseDir = "./comparer/src/main/resources/org/clulab/alignment/comparer"

  val ataInputFilename = s"$baseDir/spreadsheets/07-ATA.tsv"
  val nafInputFilename = s"$baseDir/spreadsheets/07-NAF.tsv"
  val xtraInputFilename = s"$baseDir/spreadsheets/07-XTRA.tsv"
  val inputFilenames = Seq(ataInputFilename, nafInputFilename, xtraInputFilename)

  val outputFilename = "../ExperimentSpreadsheetsApp.tsv"
  val datamartFilename = s"$baseDir/datamarts/datamarts.tsv"
  val datamartIndexFilename = s"$baseDir/indexes/index_1/hnswlib-datamart.idx"
  val searcherLocations = new SearcherLocations(1, s"$baseDir/indexes")

  val maxHits = 10
  val thresholdOpt: Option[Float] = None
  val tsvReader = new TsvReader()

  val datamartIndex: Index = getDatamartIndex(rebuild = true)
  val searcher = new Searcher(searcherLocations)

  def getSearcher: Searcher = {
    if (!searcher.isReady) {
      println("Waiting for searcher...")
      while (!searcher.isReady)
        Thread.sleep(100)
      println("Searcher ready.")
    }
    searcher
  }

  def getDatamartIndex(rebuild: Boolean): DatamartIndex.Index = {
    if (!rebuild)
      DatamartIndex.load(datamartIndexFilename)
    else {
      HnswlibDatamartIndexerApp.run(datamartFilename, datamartIndexFilename)
      DatamartIndex.load(datamartIndexFilename)
    }
  }

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
      case _ => throw new RuntimeException(s"Unexpected value for count: $count")
    }
  }

  def getDocumentIdValues(values: Array[String], cols: Array[Int]): Array[DatamartIdentifier] = {

    def unescape(string: String): String = {
      val unquoted =
        if (string.startsWith("\"") && string.endsWith("\"")) string.drop(1).dropRight(1)
        else string

      unquoted
        .replace("\\/", "\\\\")
    }

    def reescape(string: String): String = string.replace("\\\\", "/")

    val ids = cols.map(values(_)).filter(_.nonEmpty)

    if (ids.nonEmpty && (ids.head == "[Not concept node]" || ids.head == "undefined"))
      Array.empty
    else {
      val datamartIdentifiers = ids.map { id =>
        try {
          // Make sure the gold value actually exists by checking for its vector.
          val Array(datamartId, datasetId, variableId) = unescape(id).split('/')
          val datamartIdentifier = DatamartIdentifier(reescape(datamartId), reescape(datasetId), reescape(variableId))

          if (!datamartIndex.contains(datamartIdentifier))
            println(s"DatamartIdentifier doesn't exist: $datamartIdentifier.")
          datamartIdentifier
        }
        catch {
          case _: Throwable => throw new RuntimeException(s"Couldn't search for $id.")
        }
      }
      datamartIdentifiers
    }
  }

  def score(ids: Seq[DatamartIdentifier], goldIds: Seq[DatamartIdentifier]): Int = {
    val count = ids.count { id =>
      goldIds.contains(id)
    }

    count
  }

  def test(inputFilename: String, tsvWriter: TsvWriter): (Int, Int) = {
    val inputFile = new File(inputFilename)

    Sourcer.sourceFromFile(inputFile).autoClose { source =>
      val allLines = source.getLines
      val untilEmptyLines = allLines.takeWhile { line => !line.forall(_ == '\t') }
      val linesAndLineNos = untilEmptyLines.zipWithIndex
      val headers = tsvReader.readln(linesAndLineNos.next()._1)
      val conceptNameCol = headers.indexOf("Concept name")
      val ontologyNodesCol = headers.indexOf("OntologyNodes")
      val oldCols = Array(
        headers.indexOf("DatamartIdentifier1"),
        headers.indexOf("DatamartIdentifier2"),
        headers.indexOf("DatamartIdentifier3")
      )
      val goldCols = Array(
        headers.indexOf("Gold1a"),
        headers.indexOf("Gold1b"),
        headers.indexOf("Gold2a"),
        headers.indexOf("Gold2b"),
        headers.indexOf("Gold3a"),
        headers.indexOf("Gold3b")
      )
      val uazScoreCols = Array(
        headers.indexOf("Cheryl's Score for UAZ Indicators"),
        headers.indexOf("Robyn's Score for UAZ Indicators")
      )
      val colCount = headers.length
      var count = 0
      var (oldScoreSum, newScoreSum) = (0, 0)

      linesAndLineNos.foreach { case (line, zeroLineNo) =>
        val lineNo = zeroLineNo + 1
        val values = tsvReader.readln(line, colCount)
        val goldValues = getDocumentIdValues(values, goldCols)
        val oldValues = getDocumentIdValues(values, oldCols)

        if (goldValues.nonEmpty && oldValues.nonEmpty) {
          val ontologyNodes = values(ontologyNodesCol)
          val newValues = if (ontologyNodes.nonEmpty) {
            val homeIdAndAwayIdOptOpt = mkHomeAndAwayIds(ontologyNodes.split(' '))

            if (homeIdAndAwayIdOptOpt.nonEmpty) {
              val (homeId, awayIdOpt) = homeIdAndAwayIdOptOpt.get

              if (true) { // (searcher.isReady) { // for the impatient
                val datamartIdentifiersAndValues = getSearcher.runOld(homeId, awayIdOpt.toArray, maxHits, thresholdOpt).dstResults.take(3)
                val datamartIdentifiers = datamartIdentifiersAndValues.map(_._1)

                datamartIdentifiers
              }
              else
                Seq.empty[DatamartIdentifier]
            }
            else {
              val datamartIdentifiersAndValues = getSearcher.runOld(ontologyNodes, maxHits, thresholdOpt).take(3)
              val datamartIdentifiers = datamartIdentifiersAndValues.map(_._1)

              datamartIdentifiers
            }
          }
          else
            Seq.empty[DatamartIdentifier]
          val oldScore = score(oldValues, goldValues)
          val newScore = score(newValues, goldValues)
          val concept = values(conceptNameCol)
          val uazScores = uazScoreCols.map(values(_)).mkString("[", ", ", "]")

          count += 1
          oldScoreSum += oldScore
          newScoreSum += newScore
          tsvWriter.println(inputFile.getName, count.toString, lineNo.toString, concept, uazScores, oldScore.toString, newScore.toString)
        }
      }

      (oldScoreSum, newScoreSum)
    }
  }

  def run(): (Int, Int) = {
    FileUtils.printWriterFromFile(outputFilename).autoClose { printWriter =>
      val tsvWriter = new TsvWriter(printWriter)
      var (oldScoreTotal, newScoreTotal) = (0, 0)

      tsvWriter.println("File", "Index", "Line", "Compositional Grounding", "UAzScores", "OldScore", "NewScore")
      inputFilenames.foreach { inputFilename =>
        val (oldScoreSum, newScoreSum) = test(inputFilename, tsvWriter)
        oldScoreTotal += oldScoreSum
        newScoreTotal += newScoreSum
      }
      tsvWriter.println()
      tsvWriter.println("", "", "", "Totals", "", oldScoreTotal.toString, newScoreTotal.toString)
      (oldScoreTotal, newScoreTotal)
    }
  }
}

object ExperimentSpreadsheetsApp extends App {
  val (oldScoreTotal, newScoreTotal) = new ExperimentSpreadsheetsApp().run()

  println(s"OldScoreTotal: $oldScoreTotal")
  println(s"NewScoreTotal: $newScoreTotal")
}
