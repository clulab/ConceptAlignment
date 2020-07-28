//This script test the mapping from ontology node to the examples. (from column 2 to column 1)
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import ai.lum.common.ConfigUtils._
import org.clulab.alignment.Example.{aligner, tdConcepts}
import org.clulab.alignment.utils.ConceptUtils
import org.clulab.alignment.utils.ConceptUtils.ontologyHandler
import org.clulab.alignment._
import org.clulab.wm.eidos.groundings.EidosWordToVec

import scala.collection.mutable.ArrayBuffer

object TestAlignerTopDownUtils {
  // Read evaluation data from the resource folder, return all
  def readEvaluationDataFromTsv():Seq[(String, String)] = {
    val sentenceClassifierEvaluationData = ArrayBuffer[(String, String)]()

    val config = ConfigFactory.load("alignment")
    val spreadsheetPath = config.getString("AlignerTest.evaluationDataPath")

    Sourcer.sourceFromResource(spreadsheetPath).autoClose { bufferedSource =>
      for (line <- bufferedSource.getLines) {

        val cols = line.split('\t').map(_.trim)
        // do whatever you want with the columns here
        val example = cols(0).toLowerCase()
        val node = cols(1).toLowerCase()

        sentenceClassifierEvaluationData.append((example, node))
      }
    }

    sentenceClassifierEvaluationData
  }

  def generateQueryAnswerPairs(exampleNodePairsRaw:Seq[(String, String)]):Seq[(String, Seq[String])]  ={

    val queryAnswerMap_ = collection.mutable.Map[String, ArrayBuffer[String]]()

    // Get unique queries.
    exampleNodePairsRaw.map{x => x._2}.distinct.foreach{y => queryAnswerMap_(y) = ArrayBuffer[String]()}

    // Get all answers for each unique query
    exampleNodePairsRaw.foreach{x => queryAnswerMap_(x._2).append(x._1)}

    // Remove duplicate answers for each query
    val queryAnswerSeq = ArrayBuffer[(String, Seq[String])]()
    queryAnswerMap_.foreach{x => queryAnswerSeq.append((x._1, x._2.distinct))}

    queryAnswerSeq
  }

  def constructCandidateAnswerExampleEmbeddings(w2v: EidosWordToVec, queryAnswerSeq:Seq[(String, Seq[String])]):ConceptSequence = {

    val allAnswers = ArrayBuffer[String]()
    queryAnswerSeq.foreach{x => allAnswers++=x._2}
    val uniqueAnswers = allAnswers.distinct

    ConceptSequence(uniqueAnswers.map(answerString => new FlatConcept(answerString, w2v.makeCompositeVector(answerString.split(" ")))))
  }

  def ymlTextToConcept(ymlText: String, w2v:EidosWordToVec):Concept = {
    val normalizedText = ymlText.replace("/","_")
    new FlatConcept(normalizedText, w2v.makeCompositeVector(normalizedText.split("_")))
  }

  def getPrecisionAtTen(pred: Seq[ScoredPair], target: Seq[String]):Float = {

    var hitCount = 0.0f
    for (scoredPair <- pred){
      if (target.contains(scoredPair.dst.name)){
        hitCount+=1
      }
    }
    hitCount/target.length
  }
}

class TestAlignerTopDown extends FlatSpec with Matchers {

  // load evaluation data
  val rawEvaluationData = TestAlignerTopDownUtils.readEvaluationDataFromTsv()
  val topDownSamplePairSeq = TestAlignerTopDownUtils.generateQueryAnswerPairs(rawEvaluationData)

  // load aligner
  val (_, w2v) = ConceptUtils.conceptsFromWMOntology("wm_flattened")
  val aligner = WeightedParentSimilarityAligner.fromConfig(w2v)

  // load candidate answer embeddings
  val conceptSeq = TestAlignerTopDownUtils.constructCandidateAnswerExampleEmbeddings(w2v, topDownSamplePairSeq)

  // actual testing
  behavior of "Top down concept aligner"

  it should "have an precision@10 above 0.5" in {

    var precisionSeq = new ArrayBuffer[Float]()
    for (evalPair <- topDownSamplePairSeq) {
      val ymlConcept = TestAlignerTopDownUtils.ymlTextToConcept(evalPair._1, w2v)
      val top10 = aligner.topk(ymlConcept, conceptSeq, 10)
      precisionSeq.append(TestAlignerTopDownUtils.getPrecisionAtTen(top10, evalPair._2))
    }

    val precisionAt10 = precisionSeq.sum/precisionSeq.length

    precisionAt10>0.5 should be (true)
  }
}
