package org.clulab.alignment

import com.github.jelmerk.knn.scalalike.{Item, SearchResult}
import org.clulab.alignment.OntologyMapperApp.{DatamartToOntology, OntologyToDatamart}
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.data.ontology.OntologyIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.{DatamartIndex, OntologyIndex}
import org.clulab.alignment.indexer.knn.hnswlib.item.{DatamartAlignmentItem, OntologyAlignmentItem}


class OntologyMapper(val datamartIndex: DatamartIndex.Index, val ontologyIndex: OntologyIndex.Index) {

  def ontologyToDatamartMapping(topK: Int = datamartIndex.size): Iterator[Seq[OntologyToDatamart]] = {
    val srcItems = ontologyIndex.iterator
    srcItems map { item =>
      alignOntologyItemToDatamart(item, topK)
    }
  }

  def datamartToOntologyMapping(topK: Int = ontologyIndex.size): Iterator[Seq[DatamartToOntology]] = {
    val srcItems = datamartIndex.iterator
    srcItems map { item =>
      alignDatamartItemToOntology(item, topK)
    }
  }


  def alignOntologyItemToDatamart(ontologyItem: OntologyAlignmentItem, topK: Int = datamartIndex.size): Seq[OntologyToDatamart] = {
    val ontologyNodeId: OntologyIdentifier = ontologyItem.id
    val ontologyNodeVector = ontologyItem.vector
    // Find the nearest neighbors in the Datamart
    DatamartIndex.findNearest(datamartIndex, ontologyNodeVector, topK)
      .map(result => OntologyToDatamart(ontologyNodeId, result))
      .toSeq
  }

  def alignDatamartItemToOntology(datamartItem: DatamartAlignmentItem, topK: Int = ontologyIndex.size): Seq[DatamartToOntology] = {
    val datamartNodeId: DatamartIdentifier = datamartItem.id
    val datamartNodeVector = datamartItem.vector
    // Find the nearest neighbors in the Datamart
    OntologyIndex.findNearest(ontologyIndex, datamartNodeVector, topK)
      .map(result => DatamartToOntology(datamartNodeId, result))
      .toSeq
  }
}

object OntologyMapperApp extends App {
  case class OntologyToDatamart(srcId: OntologyIdentifier, dstResult: SearchResult[DatamartAlignmentItem, Float])
  case class DatamartToOntology(srcId: DatamartIdentifier, dstResult: SearchResult[OntologyAlignmentItem, Float])

  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-wm_flattened.idx"

  val datamartIndex = DatamartIndex.load(datamartFilename)
  val ontologyIndex = OntologyIndex.load(ontologyFilename)

  val mapper = new OntologyMapper(datamartIndex, ontologyIndex)


  val abc = mapper.ontologyToDatamartMapping(5)
  abc.toArray foreach { xxx =>
    println(xxx)
    // print the contents too for debug
  }
}

