package org.clulab.alignment.comparer

import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.utils.{CsvWriter, FileUtils, Sourcer}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.webapp.searcher.SearcherLocations
import org.clulab.wm.eidoscommon.utils.StringUtils

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

  while (!searcher.isReady)
    Thread.sleep(100)

  val conceptMap = {
    val conceptIndex = searcher.compositionalOntologyMapperOpt.get.conceptIndex
    val map = conceptIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val key =
          if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
          else nodeName

      println(key)
      key -> nodeName // Map nodeName without / to one with or without.
    }.toMap

    map
  }

  val propertyMap = {
    val propertyIndex = searcher.compositionalOntologyMapperOpt.get.propertyIndex
    val map = propertyIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val withoutSlash =
          if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
          else nodeName
      val key = StringUtils.afterLast(withoutSlash, '/', true)

      println(key)
      key -> nodeName
    }.toMap

    map
  }

  val processMap = {
    val processIndex = searcher.compositionalOntologyMapperOpt.get.processIndex
    val map = processIndex.map { flatOntologyAlignmentItem =>
      val nodeName = flatOntologyAlignmentItem.id.nodeName
      val withoutSlash =
        if (nodeName.endsWith("/")) StringUtils.beforeLast(nodeName, '/')
        else nodeName
      val key = StringUtils.afterLast(withoutSlash, '/', true)

      println(key)
      key -> nodeName
    }.toMap

    map
  }

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
    val homeIds = 0.until(count).flatMap { propertyCount =>
      val conceptParts = parts.dropRight(propertyCount)
      val conceptPathOpt = conceptMap.get(parent + "/" + conceptParts.mkString("_"))

      val otherParts = parts.takeRight(propertyCount)
      val propertyPathOpt = propertyMap.get(otherParts.mkString("_"))
      val processPathOpt = processMap.get(otherParts.mkString("_"))

      if (conceptPathOpt.isDefined && (otherParts.isEmpty || propertyPathOpt.isDefined || processPathOpt.isDefined)) {
        val conceptIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptPathOpt.get, Some(CompositionalOntologyIdentifier.concept))
        val homeIdOpt = {
          if (otherParts.isEmpty)
            Some(new CompositionalOntologyIdentifier(conceptIdentifier, None, None, None))
          else if (propertyPathOpt.isDefined) {
            val propertyIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyPathOpt.get, Some(CompositionalOntologyIdentifier.property))
            Some(new CompositionalOntologyIdentifier(conceptIdentifier, Some(propertyIdentifier), None, None))
          }
          else if (processPathOpt.isDefined) {
            val processIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, processPathOpt.get, Some(CompositionalOntologyIdentifier.process))
            Some(new CompositionalOntologyIdentifier(conceptIdentifier, None, Some(processIdentifier), None))
          }
          else
            None
        }

        homeIdOpt
      }
      else
        None
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
