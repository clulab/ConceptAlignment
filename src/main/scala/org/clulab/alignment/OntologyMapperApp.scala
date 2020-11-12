package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.{Item, SearchResult}
import org.clulab.alignment.OntologyMapperApp.{DatamartToOntologies, OntologyToDatamarts}
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.OntologyIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.{DatamartIndex, OntologyIndex}
import org.clulab.alignment.indexer.knn.hnswlib.item.{DatamartAlignmentItem, OntologyAlignmentItem}

class OntologyMapper(val datamartIndex: DatamartIndex.Index, val ontologyIndex: OntologyIndex.Index) {

  def ontologyToDatamartMapping(topK: Int = datamartIndex.size): Seq[OntologyToDatamarts] = {
    val ontologyItems = ontologyIndex.iterator.toList
    val ontologyToDatamarts = ontologyItems.map { ontologyItem =>
      val searchResults = alignOntologyItemToDatamart(ontologyItem, topK).toList
      OntologyToDatamarts(ontologyItem.id, searchResults)
    }
    ontologyToDatamarts
  }

  def datamartToOntologyMapping(topK: Int = ontologyIndex.size): Seq[DatamartToOntologies] = {
    val datamartItems = datamartIndex.iterator.toList
    val datamartToOntologies = datamartItems.map { datamartItem =>
      val searchResults = alignDatamartItemToOntology(datamartItem, topK).toList
      DatamartToOntologies(datamartItem.id, searchResults)
    }
    datamartToOntologies
  }

  def alignOntologyItemToDatamart(ontologyItem: OntologyAlignmentItem, topK: Int = datamartIndex.size): Iterator[SearchResult[DatamartAlignmentItem, Float]] = {
    val ontologyNodeVector = ontologyItem.vector
    val result = DatamartIndex.findNearest(datamartIndex, ontologyNodeVector, topK)
    result
  }

  def alignDatamartItemToOntology(datamartItem: DatamartAlignmentItem, topK: Int = ontologyIndex.size): Iterator[SearchResult[OntologyAlignmentItem, Float]] = {
    val datamartNodeVector = datamartItem.vector
    val result = OntologyIndex.findNearest(ontologyIndex, datamartNodeVector, topK)
    result
  }
}

object OntologyMapperApp extends App {
  case class OntologyToDatamarts(srcId: OntologyIdentifier, dstResults: Seq[SearchResult[DatamartAlignmentItem, Float]])
  case class DatamartToOntologies(srcId: DatamartIdentifier, dstResults: Seq[SearchResult[OntologyAlignmentItem, Float]])

  val datamartFilename = args.lift(0).getOrElse("../hnswlib-datamart.idx")
  val ontologyFilename = args.lift(1).getOrElse("../hnswlib-wm_flattened.idx")

  val datamartIndex = DatamartIndex.load(datamartFilename)
  val ontologyIndex = OntologyIndex.load(ontologyFilename)
  val mapper = new OntologyMapper(datamartIndex, ontologyIndex)

  val allOntologyToDatamarts = mapper.ontologyToDatamartMapping(5)
  allOntologyToDatamarts.foreach { ontologyToDatamarts =>
    println(ontologyToDatamarts.srcId)
    ontologyToDatamarts.dstResults.foreach { searchResult =>
      println(s"\t${searchResult.item}\t${searchResult.distance}")
    }
  }

  val allDatamartToOntologies = mapper.datamartToOntologyMapping(5)
  allDatamartToOntologies.foreach { datamartToOntologies =>
    println(datamartToOntologies.srcId)
    datamartToOntologies.dstResults.foreach { searchResult =>
      println(s"\t${searchResult.item}\t${searchResult.distance}")
    }
  }
}
