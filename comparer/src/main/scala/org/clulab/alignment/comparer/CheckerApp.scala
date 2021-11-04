package org.clulab.alignment.comparer

import org.clulab.alignment.data.ontology.{CompositionalOntologyIdentifier, FlatOntologyIdentifier}
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.utils.{CsvWriter, FileUtils, Sourcer, TsvWriter}
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
    println("\nconceptMapLong:\n")
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
    println("\nconceptMapShort:\n")
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
    println("\npropertyMap:\n")
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
    println("\nprocessMap:\n")
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
    homeId.conceptPropertyOntologyIdentifierOpt.foreach(arrayBuffer += _.nodeName)
    homeId.processOntologyIdentifierOpt.foreach(arrayBuffer += _.nodeName)
    homeId.processPropertyOntologyIdentifierOpt.foreach(arrayBuffer += _.nodeName)
    awayIdOpt.foreach(arrayBuffer += _.conceptOntologyIdentifier.nodeName)
    awayIdOpt.foreach { awayId =>
      awayId.conceptPropertyOntologyIdentifierOpt.foreach(arrayBuffer += _.nodeName)
    }

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

      val (propertyPathOpt, processPathOpt, conceptPathShortOpt) = {
        val tmpConceptPathShortOpt = conceptMapShort.get(otherParts.mkString("_"))
        val tmpPropertyPathOpt = propertyMap.get(otherParts.mkString("_"))
        val tmpProcessPathOpt = processMap.get(otherParts.mkString("_"))
        val tmpCount = Seq(tmpConceptPathShortOpt, tmpPropertyPathOpt, tmpProcessPathOpt).count(_.isDefined)

        if (conceptPathLongOpt.isEmpty)
          (None, None, None) // Don't even consider this.
        else if (tmpCount == 0 || tmpCount == 1)
          (tmpPropertyPathOpt, tmpProcessPathOpt, tmpConceptPathShortOpt)
        else if (tmpCount == 2) {
          val conceptEndsWithSlash = tmpConceptPathShortOpt.map(_.endsWith("/")).getOrElse(false)
          val propertyEndsWithSlash = tmpPropertyPathOpt.map(_.endsWith("/")).getOrElse(false)
          val processEndsWithSlash = tmpProcessPathOpt.map(_.endsWith("/")).getOrElse(false)

          if (tmpPropertyPathOpt.isDefined && tmpProcessPathOpt.isDefined)
            (tmpPropertyPathOpt, tmpProcessPathOpt, None)
          else if ((!propertyEndsWithSlash || !processEndsWithSlash) && conceptEndsWithSlash)
            (tmpPropertyPathOpt, tmpProcessPathOpt, None)
          else if ((propertyEndsWithSlash || processEndsWithSlash) && !conceptEndsWithSlash)
            (None, None, tmpConceptPathShortOpt)
          else // both, so disfavor the concept
            (tmpPropertyPathOpt, tmpProcessPathOpt, None)
        }
        else
          (None, None, None)
      }
      val count = Seq(conceptPathShortOpt, propertyPathOpt, processPathOpt).count(_.isDefined)

      val homeIdOptAndAwayIdOptOpt = if (count > 1) {
        println(node + " even worse!")
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

    if (homeIdAndAwayIdOpts.isEmpty) {

      val (concept1, conceptProperty, process, processProperty, concept2, conceptProperty2) = node match {
        case "wm/concept/goods/food_access_availability" => None
          ("food", "", "access", "availability", "", "")
        case "wm/concept/goods/food_insecurity_earthquake" =>
          ("food", "insecurity", "", "", "earthquake", "")
        case "wm/concept/goods/food_insecurity_nutrients" =>
          ("food", "insecurity", "", "", "nutrients", "")
        case "wm/concept/goods/food_insecurity_tension" =>
          ("food", "insecurity", "", "", "tension", "")
        case "wm/concept/goods/food_security_tension" =>
          ("food", "security", "", "", "tension", "")
        case "wm/concept/goods/food_security_threat" =>
          ("food", "security", "threat", "", "", "")
        case "wm/concept/health/vaccination_pathogen_availability" =>
          ("vaccination", "", "", "", "pathogen", "availability")
        case "wm/concept/health_intensive_care_price_or_cost" =>
          ("health", "", "", "", "intensive_care", "price_or_cost")
        case "wm/concept/agriculture/poultry/poultry_feed_price_or_cost" =>
          ("poultry", "", "", "", "feed", "price_or_cost")
        case "wm/concept/agriculture/poultry/poultry_meat_availability" =>
          ("poultry", "", "", "", "meat", "availability")
        case _ => ???
      }
      val homeId = CompositionalOntologyIdentifier(
        FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptMapShort(concept1),Some(CompositionalOntologyIdentifier.concept)),
        if (conceptProperty.isEmpty) None else Some(FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyMap(conceptProperty),Some(CompositionalOntologyIdentifier.property))),
        if (process.isEmpty) None else Some(FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, processMap(process),Some(CompositionalOntologyIdentifier.process))),
        if (processProperty.isEmpty) None else Some(FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyMap(processProperty),Some(CompositionalOntologyIdentifier.property))),
      )
      val awayIdOpt =
          if (concept2.isEmpty) None
          else
            Some(CompositionalOntologyIdentifier(
              FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, conceptMapShort(concept2),Some(CompositionalOntologyIdentifier.concept)),
              if (conceptProperty2.isEmpty) None else Some(FlatOntologyIdentifier(CompositionalOntologyIdentifier.ontology, propertyMap(conceptProperty2),Some(CompositionalOntologyIdentifier.property))),
              None,
              None
            ))

      Some((homeId, awayIdOpt))
    }
    else
      homeIdAndAwayIdOptOpt
  }

  inputFilenames.zipWithIndex.foreach { case (inputFilename, index) =>
    val nodes = readNodes(inputFilename)

    FileUtils.printWriterFromFile(outputFilenames(index)).autoClose { printWriter =>
      val xsvWriter = new TsvWriter(printWriter)

      xsvWriter.println("OldNode", "NewNodes", "VariableId1", "VariableId2", "VariableId3")

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
                      println(node + " not found")
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
        xsvWriter.println(Seq(node, nodeName) ++ variableIds.take(3))
      }
    }
  }
}
