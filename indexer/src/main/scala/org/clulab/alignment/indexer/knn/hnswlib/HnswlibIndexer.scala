package org.clulab.alignment.indexer.knn.hnswlib

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import java.io.{File, FileInputStream}
import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.data.ontology.FlatOntologyIdentifier
import org.clulab.alignment.embedder.{DatamartAverageEmbedder, DatamartEmbedder, DatamartEpsWeightedAverageEmbedder, DatamartExpWeightedAverageEmbedder, DatamartPowWeightedAverageEmbedder, DatamartSingleEmbedder, DatamartStopwordEmbedder, DatamartWeightedAverageEmbedder, DatamartWordEmbedder}
import org.clulab.alignment.grounder.datamart.DatamartOntology
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.GloveIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.FlatOntologyIndex
import org.clulab.alignment.indexer.knn.hnswlib.index.SampleIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.GloveAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.FlatOntologyAlignmentItem
import org.clulab.alignment.indexer.knn.hnswlib.item.SampleAlignmentItem
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.OntologyHandlerHelper
import org.clulab.embeddings.{CompactWordEmbeddingMap, WordEmbeddingMapPool}
import org.clulab.wm.eidos.EidosSystem
import org.clulab.wm.eidos.groundings.OntologyHandler
import org.clulab.wm.eidos.groundings.grounders.EidosOntologyGrounder
import org.clulab.wm.eidoscommon.{EidosProcessor, EidosTokenizer}
import org.clulab.wm.ontologies.NodeTreeDomainOntologyBuilder

import scala.collection.JavaConverters._

class HnswlibIndexer {
  val dimensions = 300
  val w2v: CompactWordEmbeddingMap = HnswlibIndexer.w2v
  val datamartEmbedder: DatamartEmbedder = getEmbedder

  def getEmbedder: DatamartEmbedder = {
    // Pick one of these.
    // new DatamartAverageEmbedder(w2v)
    DatamartEpsWeightedAverageEmbedder(w2v)
    // DatamartExpWeightedAverageEmbedder(w2v)
    // DatamartPowWeightedAverageEmbedder(w2v)
    // new DatamartSingleEmbedder(w2v)
    // new DatamartStopwordEmbedder(w2v)
    // new DatamartWordEmbedder(w2v)
  }

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

  def indexGlove(indexFilename: String, countOpt: Option[Int] = None): GloveIndex.Index = {
    val allKeys = (w2v.keys + "").toSeq.sorted // Always insert in the same order!
    val keys = countOpt.map(allKeys.take).getOrElse(allKeys)
    val items = keys.map { key => GloveAlignmentItem(key, w2v.getOrElseUnknown(key).toArray) }
    val index = GloveIndex.newIndex(items)

    index.save(new File(indexFilename))
    index
  }

  def readFlatOntologyItems(): Seq[FlatOntologyAlignmentItem] = {
    val namespace = "wm_flattened"
    val config = ConfigFactory
      .empty
      .withValue("ontologies.ontologies", ConfigValueFactory.fromIterable(
        // Both of these are needed and Eidos isn't configured that way by default.
        Seq(namespace).asJava
      ))
      .withFallback(EidosSystem.defaultConfig)

    val ontologyHandler = OntologyHandlerHelper.fromConfig(config)
    val eidosOntologyGrounder = ontologyHandler.ontologyGrounders
        .collect { case grounder: EidosOntologyGrounder => grounder}
        .find { grounder => grounder.name == namespace }
        .get
    val conceptEmbeddings = eidosOntologyGrounder.conceptEmbeddings
    val items: Seq[FlatOntologyAlignmentItem] = conceptEmbeddings.map { conceptEmbedding =>
      val name = conceptEmbedding.namer.getName
      val branchOpt = conceptEmbedding.namer.getBranchOpt
      val embedding = conceptEmbedding.embedding
      val identifier = FlatOntologyIdentifier(namespace, name, branchOpt)

      FlatOntologyAlignmentItem(identifier, embedding)
    }

    items
  }

  def indexFlatOntology(indexFilename: String): FlatOntologyIndex.Index = {
    // Turn off warnings from this class.
    edu.stanford.nlp.ie.NumberNormalizer.setVerbose(false)

    val items = readFlatOntologyItems()
    val index = FlatOntologyIndex.newIndex(items)

    index.save(new File(indexFilename))
    index
  }

  def newOntologyHandler(ontologyFilename: String, version: String, oldOntologyHandler: OntologyHandler, namespace: String, oldTokenizer: EidosTokenizer): OntologyHandler = {
    val file = new File(ontologyFilename)
    val oldSentencesExtractor = oldOntologyHandler.sentencesExtractor
    val oldCanonicalizer = oldOntologyHandler.canonicalizer
    val newDomainOntologyBuilder = new NodeTreeDomainOntologyBuilder(oldSentencesExtractor, oldCanonicalizer, filtered = true)
    val newDomainOntology = new FileInputStream(file).autoClose { inputStream =>
      newDomainOntologyBuilder.buildFromStream(inputStream, Some(version), None)
    }
    val newOntologyGrounder = EidosOntologyGrounder.mkGrounder(namespace, newDomainOntology, oldOntologyHandler.wordToVec, oldOntologyHandler.canonicalizer, oldTokenizer)
    val newOntologyHandler = new OntologyHandler(
      Seq(newOntologyGrounder),
      oldOntologyHandler.wordToVec,
      oldOntologyHandler.sentencesExtractor,
      oldOntologyHandler.canonicalizer,
      oldOntologyHandler.includeParents,
      oldOntologyHandler.topN,
      oldOntologyHandler.threshold
    )

    newOntologyHandler
  }

  def indexCompositionalOntology(conceptIndexFilename: String, processIndexFilename: String,
      propertyIndexFilename: String, ontologyFilenameOpt: Option[String] = None, ontologyIdOpt: Option[String] = None): Seq[FlatOntologyIndex.Index] = {
    val conceptBranchAndFilename = ("concept", conceptIndexFilename)
    val processBranchAndFilename = ("process", processIndexFilename)
    val propertyBranchAndFilename = ("property", propertyIndexFilename)
    val branchesAndFilenames = Seq(conceptBranchAndFilename, processBranchAndFilename, propertyBranchAndFilename)
    val namespace = "wm_compositional"
    val ontologyHandler = ontologyFilenameOpt.map { ontologyFilename =>
      val config = ConfigFactory
          .empty
          .withValue("ontologies.ontologies", ConfigValueFactory.fromIterable(
            Seq().asJava // Do not preload any for efficiency.
          ))
          .withFallback(EidosSystem.defaultConfig)
      val oldOntologyHandler = OntologyHandlerHelper.fromConfig(config)
      val language = config.getString("EidosSystem.language")
      val eidosProcessor = EidosProcessor(language, cutoff = 150) // How expensive is this?
      val oldTokenizer = eidosProcessor.getTokenizer

      newOntologyHandler(ontologyFilename, ontologyIdOpt.get, oldOntologyHandler, namespace, oldTokenizer)
    }.getOrElse {
      val config = ConfigFactory
          .empty
          .withValue("ontologies.ontologies", ConfigValueFactory.fromIterable(
            Seq("wm_compositional").asJava
          ))
          .withFallback(EidosSystem.defaultConfig)

      OntologyHandlerHelper.fromConfig(config)
    }
    val eidosOntologyGrounder = ontologyHandler.ontologyGrounders
        .collect { case grounder: EidosOntologyGrounder => grounder}
        .find { grounder => grounder.name == namespace }
        .get
    val allConceptEmbeddings = eidosOntologyGrounder.conceptEmbeddings
    val branchedConceptEmbeddings = allConceptEmbeddings.groupBy(_.namer.getBranchOpt.get)
    val indexes = branchesAndFilenames.map { case (branch, indexFilename) =>
      val conceptEmbeddings = branchedConceptEmbeddings(branch)
      val items = conceptEmbeddings.map { conceptEmbedding =>
        val name = conceptEmbedding.namer.getName
        val branchOpt = conceptEmbedding.namer.getBranchOpt
        val embedding = conceptEmbedding.embedding
        val identifier = FlatOntologyIdentifier(namespace, name, branchOpt)

        FlatOntologyAlignmentItem(identifier, embedding)
      }
      val index = FlatOntologyIndex.newIndex(items)

      index.save(new File(indexFilename))
      index
    }

    indexes
  }

  def getComplexEmbedding(words: Array[String]): Array[Float] = {
    null
  }

  def indexDatamart(datamartFilename: String, indexFilename: String): DatamartIndex.Index = {
    val tokenizer = Tokenizer()
    val ontology = DatamartOntology.fromFile(datamartFilename, tokenizer)
    val items = ontology.datamartEntries.map { datamartEntry =>
      val identifier = datamartEntry.identifier
      val embedding = datamartEmbedder.embed(datamartEntry)

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

object HnswlibIndexer {
  lazy val eidos: EidosSystem = new EidosSystem()


  // This needs to be coordinated with processors or at least build.sbt.
  lazy val w2v: CompactWordEmbeddingMap = {
    // If Eidos has been loaded, get its map.
//    eidos
//        .components
//        .ontologyHandlerOpt
//        .get
//        .wordToVec
//        .asInstanceOf[RealWordToVec]
//        .w2v
//        .asInstanceOf[CompactWordEmbeddingMap]

    // Without Eidos, one can be arranged via processors.
    WordEmbeddingMapPool
        .getOrElseCreate("/org/clulab/glove/glove.840B.300d.10f", true)
        .asInstanceOf[CompactWordEmbeddingMap]
  }
}
