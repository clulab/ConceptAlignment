package org.clulab.alignment.comparer

import org.clulab.alignment.CompositionalOntologyToDatamarts
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.{CsvWriter, FileUtils, Sourcer}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.webapp.searcher.SearcherLocations
import org.clulab.wm.eidoscommon.utils.{StringUtils, TsvReader}

object CheckerApp extends App {
  val ataInputFilename = "../comparer/ATA2.csv"
  val nafInputFilename = "../comparer/NAF2.csv"
  val inputFilenames = Seq(ataInputFilename, nafInputFilename)

  val ataOutputFilename = "../comparer/ATA-out.tsv"
  val nafOutputFilename = "../comparer/NAF-out.tsv"
  val outputFilenames = Seq(ataOutputFilename, nafOutputFilename)

  val hits = 10
  val thresholdOpt = None

  val searcherLocations = new SearcherLocations(1, "./Docker")
  val searcher = new Searcher(searcherLocations)

  def readNodes(filename: String): Array[String] = {
    Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.map { line =>
        StringUtils.beforeFirst(line, ',', true)
      }.toArray
    }
  }

  inputFilenames.zipWithIndex.foreach { case (inputFilename, index) =>
    val nodes = readNodes(inputFilename)

    FileUtils.printWriterFromFile(outputFilenames(index)).autoClose { printWriter =>
      val csvWriter = new CsvWriter(printWriter)

      nodes.foreach { node =>
        val variableIds =
            if (node.startsWith("wm/")) {
              val nodes = node.split('/')
              if (nodes(1) == "concept") {
                val conceptIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, node, Some(CompositionalOntologyIdentifier.concept))
                val homeId = new CompositionalOntologyIdentifier(conceptIdentifier, None, None, None)
                val awayIds = Array.empty[CompositionalOntologyIdentifier]
                try {
                  searcher
                      .run(homeId, awayIds, hits, thresholdOpt)
                      .dstResults
                      .map { case (datamartIdentifier, _) => datamartIdentifier.variableId }
                }
                catch {
                  case _: Exception =>
                    println(node)
                    Seq.empty[String]
                }
              }
              else
                Seq.empty[String]
            }
            else
              searcher
                  .run(node, hits, thresholdOpt)
                  .map { case (datamartDocument, _ ) => datamartDocument.variableId }

        csvWriter.println(Seq(node) ++ variableIds.take(2))
      }
    }
  }
}
