package org.clulab.alignment.comparer

import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.utils.{CsvWriter, FileUtils, Sourcer}
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.webapp.searcher.SearcherLocations
import org.clulab.wm.eidoscommon.utils.StringUtils

import scala.collection.mutable.ArrayBuffer

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

  val conceptMapLong = {
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

  val conceptMapShort = {
    val conceptIndex = searcher.compositionalOntologyMapperOpt.get.conceptIndex
    val map = conceptIndex.map { flatOntologyAlignmentItem =>
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

  def getNodeName(node: String, homeId: CompositionalOntologyIdentifier, awayIdOpt: Option[CompositionalOntologyIdentifier]): String = {
    val arrayBuffer = new ArrayBuffer[String]()

    arrayBuffer += homeId.conceptOntologyIdentifier.nodeName
    homeId.conceptPropertyOntologyIdentifierOpt.map(arrayBuffer += _.nodeName)
    homeId.processOntologyIdentifierOpt.map(arrayBuffer += _.nodeName)
    assert(homeId.processPropertyOntologyIdentifierOpt.isEmpty)
    awayIdOpt.map(arrayBuffer += _.conceptOntologyIdentifier.nodeName)

    val composedName = arrayBuffer
      .map { string =>
        val withoutSlash = if (string.endsWith("/")) string.dropRight(1) else string
        StringUtils.afterLast(withoutSlash, '/', all = true)
      }
      .mkString("_")
    val actualName = StringUtils.afterLast(node, '/', all = true)

    if (composedName != actualName)
      println("On ho!")
    arrayBuffer.mkString(" ")
  }

  def getHomeIdOpt(node: String, searcher: Searcher): Option[(CompositionalOntologyIdentifier, Option[CompositionalOntologyIdentifier])] = {
    val parent = StringUtils.beforeLast(node, '/', all = false)
    val name = StringUtils.afterLast(node, '/', all = true)
    val parts = name.split('_')
    val count = parts.length
    val homeIdAndAwayIdOpts = 0.until(count).flatMap { propertyCount =>
      val conceptParts = parts.dropRight(propertyCount)
      val conceptPathLongOpt = conceptMapLong.get(parent + "/" + conceptParts.mkString("_"))

      val otherParts = parts.takeRight(propertyCount)
      val conceptPathShortOpt = conceptMapShort.get(otherParts.mkString("_"))
      val propertyPathOpt = propertyMap.get(otherParts.mkString("_"))
      val processPathOpt = processMap.get(otherParts.mkString("_"))
      val count = Seq(conceptPathShortOpt, propertyPathOpt, processPathOpt).count(_.isDefined)

      val homeIdOptAndAwayIdOptOpt = if (count > 1) {
        println("Even worse!")
        None
      }
      else {
        if (conceptPathLongOpt.isDefined && (otherParts.isEmpty || conceptPathShortOpt.isDefined || propertyPathOpt.isDefined || processPathOpt.isDefined)) {
          val conceptIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptPathLongOpt.get, Some(CompositionalOntologyIdentifier.concept))
          val (homeIdOpt, awayIdOpt) = {
            if (otherParts.isEmpty)
              (Some(new CompositionalOntologyIdentifier(conceptIdentifier, None, None, None)), None)
            else if (conceptPathShortOpt.isDefined) {
              val conceptShortIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptPathShortOpt.get, Some(CompositionalOntologyIdentifier.concept))
              val homeIdOpt = Some(new CompositionalOntologyIdentifier(conceptIdentifier, None, None, None))
              val awayIdOpt = Some(new CompositionalOntologyIdentifier(conceptShortIdentifier, None, None, None))
              (homeIdOpt, awayIdOpt)
            }
            else if (propertyPathOpt.isDefined) {
              val propertyIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyPathOpt.get, Some(CompositionalOntologyIdentifier.property))
              (Some(new CompositionalOntologyIdentifier(conceptIdentifier, Some(propertyIdentifier), None, None)), None)
            }
            else if (processPathOpt.isDefined) {
              val processIdentifier = FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, processPathOpt.get, Some(CompositionalOntologyIdentifier.process))
              (Some(new CompositionalOntologyIdentifier(conceptIdentifier, None, Some(processIdentifier), None)), None)
            }
            else
              (None, None)
          }

          if (homeIdOpt.isEmpty && awayIdOpt.isEmpty) None
          else Some(homeIdOpt.get, awayIdOpt)
        }
        else
          None
      }
      homeIdOptAndAwayIdOptOpt
    }
    val homeIdAndAwayIdOptOpt = homeIdAndAwayIdOpts.find { case (homeId, awayIdOpt) =>
      try {
        searcher.run(homeId, awayIdOpt.toArray, hits, thresholdOpt)
        true
      }
      catch {
        case _: Exception => false
      }
    }

    homeIdAndAwayIdOptOpt
  }

  inputFilenames.zipWithIndex.foreach { case (inputFilename, index) =>
    val nodes = readNodes(inputFilename)

    FileUtils.printWriterFromFile(outputFilenames(index)).autoClose { printWriter =>
      val csvWriter = new CsvWriter(printWriter)

      nodes.foreach { node =>
        val nodeNameAndVariableIds =
            if (node.startsWith("wm/")) {
              val nodes = node.split('/')
              if (nodes.lift(1).map(_ == "concept").getOrElse(false)) {
                val homeIdAndAwayIdOptOpt = getHomeIdOpt(node, searcher)

                homeIdAndAwayIdOptOpt
                    .map { case (homeId, awayIdOpt) =>
                      val variableIds = searcher
                          .run(homeId, awayIdOpt.toArray, hits, thresholdOpt)
                          .dstResults
                          .map { case (datamartIdentifier, _) => datamartIdentifier.variableId }
                      (getNodeName(node, homeId, awayIdOpt), variableIds)
                    }
                    .getOrElse {
                      println(node)
                      (node, Seq("[Ontology node not found]"))
                    }
              }
              else
                (node, Seq("[Not concept node]"))
            }
            else {
              val variableIds = searcher
                  .run(node, hits, thresholdOpt)
                  .map { case (datamartDocument, _ ) => datamartDocument.variableId }

              (node, variableIds)
            }

        val (nodeName, variableIds) = nodeNameAndVariableIds
        csvWriter.println(Seq(nodeName) ++ variableIds.take(2))
      }
    }
  }
}
