//This script test the mapping from ontology node to the examples. (from column 2 to column 1)
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import ai.lum.common.ConfigUtils._
import org.clulab.alignment.evaluator.ExampleApp.{aligner, tdConcepts}
import org.clulab.alignment.utils.ConceptUtils
import org.clulab.alignment.utils.ConceptUtils.ontologyHandler
import org.clulab.alignment._
import org.clulab.alignment.aligner.ScoredPair
import org.clulab.alignment.aligner.WeightedParentSimilarityAligner
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.EidosWordToVec

import scala.collection.mutable.ArrayBuffer

object TestAlignerBottomUpUtils {
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

  def generateQueryAnswerPairs(exampleNodePairsRaw:Seq[(String, String)]):Seq[(String, String)]  ={

    val queryAnswerSeq = new ArrayBuffer[(String, String)]()

    for (rawTuple <- exampleNodePairsRaw){
      if (rawTuple._2.length>0){
        if (rawTuple._2.last.toString =="?"){
          queryAnswerSeq.append((rawTuple._1, rawTuple._2.replace("?", "")))
        }
        else{
          queryAnswerSeq.append(rawTuple)
        }
      }
    }
    queryAnswerSeq
  }

  def textToConcept(text:String, w2v: CompactWord2Vec):Concept = {
    new FlatConcept(text, w2v.makeCompositeVector(text.split(" ")))
  }

  def getPrecisionAtTen(pred: Seq[ScoredPair], target: String):Float = {
    println("===============")
    println("query:", pred.head.src.name)
    println("predictions:", pred.map{x => x.dst.name})
    println("target:", target)

    var hitCount = 0
    for (scoredPair <- pred){
      if (scoredPair.dst.name.contains(target)){
        hitCount+=1
      }
    }
    if (hitCount>0){ 1.0f} else {0.0f}
  }
}

class TestAlignerBottomUp extends FlatSpec with Matchers {

  // load evaluation data
  val rawEvaluationData = TestAlignerBottomUpUtils.readEvaluationDataFromTsv()
  val bottomUpSamplePairSeq = TestAlignerBottomUpUtils.generateQueryAnswerPairs(rawEvaluationData)

  bottomUpSamplePairSeq.map{x => println(x._1, x._2)}
  println("=======================")

  // load aligner
  val tdConcepts = ConceptUtils.conceptsFromWMOntology("wm_flattened")
  val w2v: CompactWord2Vec = CompactWord2Vec("/org/clulab/glove/glove.840B.300d.txt", resource = true, cached = false)
  val aligner = WeightedParentSimilarityAligner.fromConfig(w2v)

  // actual testing
  behavior of "Bottom up concept aligner"

  it should "have an precision@10 above 0.5" in {

    var precisionSeq = new ArrayBuffer[Float]()
    for (evalPair <- bottomUpSamplePairSeq) {
      val queryConcept = TestAlignerBottomUpUtils.textToConcept(evalPair._1, w2v)
      val top10 = aligner.topk(queryConcept, tdConcepts, 10)
      precisionSeq.append(TestAlignerBottomUpUtils.getPrecisionAtTen(top10, evalPair._2))
      println("precision@10:", precisionSeq.last)
    }

    val precisionAt10 = precisionSeq.sum/precisionSeq.length

    println("pr at 10:",precisionAt10)
    precisionAt10>0.5 should be (true)
  }
}

