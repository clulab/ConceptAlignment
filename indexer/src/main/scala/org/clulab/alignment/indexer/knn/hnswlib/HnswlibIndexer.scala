package org.clulab.alignment.indexer.knn.hnswlib

import java.io.File

import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.data.ontology.OntologyIdentifier
import org.clulab.alignment.grounder.datamart.DatamartOntology
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.OntologyIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.SampleIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.GloveAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.OntologyAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.SampleAlignmentItem
import org.clulab.alignment.utils.{OntologyHandlerHelper => OntologyHandler}
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.EidosOntologyGrounder

class HnswlibIndexer {
  lazy val w2v = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
  val dimensions = 300

  // This is just for testing.
  def indexSample(): Unit = {
    val items = Array(
      SampleAlignmentItem("one",   Array(1f, 2f, 3f, 4f)),
      SampleAlignmentItem("two",   Array(2f, 3f, 4f, 5f)),
      SampleAlignmentItem("three", Array(3f, 4f, 5f, 6f))
    )
    val index = SampleIndex.newIndex(items)
    val filename = "../hnswlib-sample.idx"

    index.save(new File(filename))
  }

  def indexGlove(indexFilename: String): GloveIndex.Index = {
    val keys = w2v.keys
    val items = keys.map { key => GloveAlignmentItem(key, w2v.get(key).get) }
    val index = GloveIndex.newIndex(items)

    index.save(new File(indexFilename))
    index
  }

  def indexOntology(): Unit = {
    val namespace = "wm_flattened"
    val ontologyHandler = OntologyHandler.fromConfig()
    val eidosOntologyGrounder = ontologyHandler.ontologyGrounders
        .collect { case grounder: EidosOntologyGrounder => grounder}
        .find { grounder => grounder.name == namespace }
        .get
    val conceptEmbeddings = eidosOntologyGrounder.conceptEmbeddings
    val filename = s"../hnswlib-$namespace.idx"
    val items = conceptEmbeddings.map { conceptEmbedding =>
      val name = conceptEmbedding.namer.name
      val branch = conceptEmbedding.namer.branch
      val embedding = conceptEmbedding.embedding
      val identifier = new OntologyIdentifier(namespace, name, branch)

      OntologyAlignmentItem(identifier, embedding)
    }
    val index = OntologyIndex.newIndex(items)

    index.save(new File(filename))
  }

  def indexDatamart(datamartFilename: String, indexFilename: String): DatamartIndex.Index = {
    val tokenizer = Tokenizer()
    val ontology = DatamartOntology.fromFile(datamartFilename, tokenizer)
    val items = ontology.datamartEntries.map { datamartEntry =>
      val identifier = datamartEntry.identifier
      val words = datamartEntry.words
      val embedding = w2v.makeCompositeVector(words)

      DatamartAlignmentItem(identifier, embedding)
    }
    val index = DatamartIndex.newIndex(items)

    index.save(new File(indexFilename))
    index
  }

//  indexSample()
//  indexOntology()
//  indexGlove()
//  indexDatamart()
}
