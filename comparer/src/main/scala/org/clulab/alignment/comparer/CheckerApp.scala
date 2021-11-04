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

  def getHomeIdOpt(node: String, searcher: Searcher): Option[CompositionalOntologyIdentifier] = {
    val parent = StringUtils.beforeLast(node, '/', all = false)
    val name = StringUtils.afterLast(node, '/', all = true)
    val parts = name.split('_')
    val count = parts.length
    val homeIds = 0.until(count).map { propertyCount =>
      val conceptParts = parts.dropRight(propertyCount)
      val propertyParts = parts.takeRight(propertyCount)

      val conceptPath = parent + "/" + conceptParts.mkString("_")
      val propertyPath = "wm/property/" + propertyParts.mkString("_")

      val conceptIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptPath, Some(CompositionalOntologyIdentifier.concept))
      val propertyIdentifierOpt =
        if (propertyParts.isEmpty) None
        else Some(FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyPath, Some(CompositionalOntologyIdentifier.property)))
      val homeId = new CompositionalOntologyIdentifier(conceptIdentifier, propertyIdentifierOpt, None, None)

      homeId
    }
    val homeIdOpt = homeIds.find { homeId =>
      try {
        searcher.run(homeId, Array.empty[CompositionalOntologyIdentifier], hits, thresholdOpt)
        true
      }
      catch {
        case _: Exception => false
      }
    }

    homeIdOpt
  }


  inputFilenames.zipWithIndex.foreach { case (inputFilename, index) =>
    val nodes = readNodes(inputFilename)

    FileUtils.printWriterFromFile(outputFilenames(index)).autoClose { printWriter =>
      val csvWriter = new CsvWriter(printWriter)

      nodes.foreach { node =>
        val variableIds =
            if (node.startsWith("wm/")) {
              val nodes = node.split('/')
              if (nodes.lift(1).map(_ == "concept").getOrElse(false)) {
                val homeIdOpt = getHomeIdOpt(node, searcher)

                homeIdOpt
                    .map { homeId =>
                      searcher
                          .run(homeId, Array.empty[CompositionalOntologyIdentifier], hits, thresholdOpt)
                          .dstResults
                          .map { case (datamartIdentifier, _) => datamartIdentifier.variableId }
                    }
                    .getOrElse {
                      println(node)
                      Seq("[Ontology node not found]")
                    }
              }
              else
                Seq("[Not concept node]")
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
