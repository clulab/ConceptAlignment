package org.clulab.alignment.comparer

import org.clulab.alignment.DatamartToCompositionalOntologies
import org.clulab.alignment.data.ontology.FlatOntologyIdentifier
import org.clulab.alignment.webapp.searcher.{Searcher, SearcherLocations}

import scala.collection.mutable

object FindUnusedOntologyNodesApp extends App {

  class Counter(strings: Seq[String]) {
    val map = new mutable.HashMap[String, Int]() {
      strings.foreach { string =>
        this += string -> 0
      }
    }

    def count(string: String): Unit = map(string) = map.getOrElse(string, 0) + 1

    def getCounts: Seq[(String, Int)] = map.toSeq
  }

  val maxHits = 10
  val thresholdOpt = Some(0.6f)

  val searcherLocations = new SearcherLocations(1, "../builder")
  val searcher = new Searcher(searcherLocations)

  while (!searcher.isReady)
    Thread.sleep(100)

  val compositionalOntologyMapper = searcher.compositionalOntologyMapperOpt.get
  val conceptNodeNames = new Shared.OntologyNodeReader(compositionalOntologyMapper.conceptIndex, "conceptSeq").read()
  val propertyNodeNames = new Shared.OntologyNodeReader(compositionalOntologyMapper.propertyIndex, "propertySeq").read()
  val processNodeNames = new Shared.OntologyNodeReader(compositionalOntologyMapper. processIndex, "processSeq").read()
  val ontologyNodeNames = conceptNodeNames ++ propertyNodeNames ++ processNodeNames
  val counter = new Counter(ontologyNodeNames)
  val allDatamartToOntologies = compositionalOntologyMapper.datamartToOntologyMapping(Some(maxHits), thresholdOpt)

  def countResults(searchResults: Seq[(FlatOntologyIdentifier, Float)]): Unit = {
    searchResults.foreach { searchResult =>
      val nodeName = searchResult._1.nodeName

      if (!nodeName.endsWith("/"))
        counter.count(nodeName)
    }
  }

  allDatamartToOntologies.foreach { datamartToOntologies: DatamartToCompositionalOntologies =>
    countResults(datamartToOntologies.conceptSearchResults)
    countResults(datamartToOntologies.propertySearchResults)
    countResults(datamartToOntologies.processSearchResults)
  }

  counter
    .getCounts
    .sortBy { case (nodeName, count) => (count, nodeName) }
    .foreach { case (nodeName, count) =>
      println(s"$count\t$nodeName")
    }
}
