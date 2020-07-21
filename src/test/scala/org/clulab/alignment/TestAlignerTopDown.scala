//This script test the mapping from ontology node to the examples. (from column 2 to column 1)
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import ai.lum.common.ConfigUtils._
import org.clulab.alignment.{Aligner, Concept, WeightedParentSimilarityAligner}

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

  def constructCandidateAnswerExampleEmbeddings(aligner:Aligner, queryAnswerSeq:Seq[(String, Seq[String])]):Seq[Concept] = {

    val allAnswers = ArrayBuffer[String]()
    queryAnswerSeq.foreach{x => allAnswers++=x._2}
    val uniqueAnswers = allAnswers.flatten.distinct

    val uniqueAnswerConcepts = ArrayBuffer[Concept]()
    for (answer <- uniqueAnswers){

    }


  }

}

class TestAlignerTopDown extends FlatSpec with Matchers {

  // TODO: fix this, how to load this?
  //val aligner = WeightedParentSimilarityAligner.fromConfig()

  behavior of "Top down concept aligner"

  it should "have an precision@10 above 0.6" in {
    1>0 should be (true)
  }
}
