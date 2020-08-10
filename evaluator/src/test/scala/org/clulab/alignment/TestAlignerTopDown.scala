//This script test the mapping from ontology node to the examples. (from column 2 to column 1)
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest._
import org.clulab.wm.eidos.utils.Sourcer
import org.clulab.wm.eidos.utils.Closer.AutoCloser
import ai.lum.common.ConfigUtils._
import org.clulab.alignment.evaluator.ExampleApp.{aligner}
import org.clulab.alignment.utils.ConceptUtils
import org.clulab.alignment.utils.ConceptUtils.ontologyHandler
import org.clulab.alignment._
import org.clulab.alignment.aligner.ScoredPair
import org.clulab.alignment.aligner.WeightedParentSimilarityAligner
import org.clulab.embeddings.word2vec.CompactWord2Vec
import org.clulab.wm.eidos.groundings.EidosWordToVec

import scala.collection.mutable.ArrayBuffer

object TestAlignerTopDownUtils {
  val tdConcepts = ConceptUtils.conceptsFromWMOntology("wm_flattened")

  // Some of the node names in the evaluation spreadsheet is not canonical, thus could not be found in the WM yml file
  // This mapping is used to map the non-canonical node names to the canonical ones.
  val nodeQueryReformatMap = Map(
    "train/agriculture_training/production_practices/planting"	-> "wm/concept/causal_factor/interventions/train/agriculture_training/production_practices/planting_training",
    "legislate" ->	"wm/concept/causal_factor/interventions/legislate/",
    "provide/agriculture_training/production_practices"	-> "wm/concept/causal_factor/interventions/train/agriculture_training/production_practices/",
    "provide/livelihood_support/humanitarian_non_food_items" ->	"wm/concept/causal_factor/interventions/provide/livelihood_support/humanitarian_nonfood_items/",
    "build/shelte" ->	"wm/concept/causal_factor/interventions/build/shelter_housing",
    "provide/information_services/human_rights_monitoring" ->	"wm/concept/causal_factor/interventions/provide/information_services/human_rights_monitoring/",
    "build/agriculture_infrastructure/livestock_production" ->	"wm/concept/causal_factor/interventions/build/agriculture_infrastructure/livestock_production/",
    "provide/agriculture_inputs/crop_production/weed_pest_control" -> "wm/concept/causal_factor/interventions/provide/agriculture_inputs/crop_production/weed_pest_control/",
    "provide/livelihood_support/nutrition_suppor" -> "wm/concept/causal_factor/interventions/provide/livelihood_support/nutrition_support",
    "provide/agriculture_inputs/livestock_production" -> "wm/concept/causal_factor/interventions/provide/agriculture_inputs/livestock_production/",
    "provide/crop_production/soil_inputs/fertilizer" -> "wm/concept/causal_factor/interventions/provide/agriculture_inputs/crop_production/soil_inputs/fertilizer",
    "provide/livelihood_support/humanitarian_non_food_items/shelte" -> "wm/concept/causal_factor/interventions/provide/livelihood_support/humanitarian_nonfood_items/shelter",
    "provide/crop_production/soil_inputs/fertilizer_subsidy" -> "wm/concept/causal_factor/interventions/provide/agriculture_inputs/crop_production/soil_inputs/fertilizer_subsidy",
    "build/wash_infrastructure" -> "wm/concept/causal_factor/interventions/build/WASH_infrastructure",
    "train/agriculture_training/production_practices" -> "wm/concept/causal_factor/interventions/train/agriculture_training/production_practices/",
    "train/agriculture_training" -> "wm/concept/causal_factor/interventions/train/agriculture_training/",
    "provide/medical_inputs/medical_treatment" -> "wm/concept/causal_factor/interventions/provide/medical_inputs/medical_treatment",
    "provide/medical_inputs/prevention" -> "wm/concept/causal_factor/interventions/provide/medical_inputs/prevention",
    "train/medical_training" -> "wm/concept/causal_factor/interventions/train/medical_training/",
    "provide/medical_inputs/medical_treatmen" -> "wm/concept/causal_factor/interventions/provide/medical_inputs/medical_treatment",
    "provide/livelihood_support/humanitarian_non_food_items/shelter" -> "wm/concept/causal_factor/interventions/provide/livelihood_support/humanitarian_nonfood_items/shelter"
  )

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

    // Remove the nan node
    queryAnswerMap_.remove("nan")
    println("nan here?", queryAnswerMap_.contains("nan"))

    // Split the query to two separate queries
    queryAnswerMap_("provide/medical_inputs/medical_treatment") = queryAnswerMap_("provide/medical_inputs/medical_treatment or provide/medical_inputs/prevention")
    queryAnswerMap_("provide/medical_inputs/prevention") =  queryAnswerMap_("provide/medical_inputs/medical_treatment or provide/medical_inputs/prevention")
    queryAnswerMap_.remove("provide/medical_inputs/medical_treatment or provide/medical_inputs/prevention")

    // Remove duplicate answers for each query
    val queryAnswerSeq = ArrayBuffer[(String, Seq[String])]()
    queryAnswerMap_.foreach{x => queryAnswerSeq.append((x._1, x._2.distinct))}

    queryAnswerSeq
  }

  def constructCandidateAnswerExampleEmbeddings(w2v: CompactWord2Vec, queryAnswerSeq:Seq[(String, Seq[String])]):ConceptSequence = {

    val allAnswers = ArrayBuffer[String]()
    queryAnswerSeq.foreach{x => allAnswers++=x._2}
    val uniqueAnswers = allAnswers.distinct

    ConceptSequence(uniqueAnswers.map(answerString => new FlatConcept(answerString, w2v.makeCompositeVector(answerString.split(" ")))))
  }

  def constructNodeEmbeddingMappingsFromWMNodeEmbeddings():Map[String, Int]= {
    val nodeNameIndexMap = scala.collection.mutable.Map[String, Int]()
    var conceptIdx = 0

    println("we are here?")
    for (concept <- tdConcepts.concepts){
      nodeNameIndexMap(concept.name) = conceptIdx
      conceptIdx+=1
    }
    nodeNameIndexMap.toMap
  }

  def getQueryConcept(wmConcepts:ConceptSequence, nodeNameIndexMap:Map[String,Int], queryRawText:String): Concept = {
    println("node in wm sheet? ", nodeQueryReformatMap.contains(queryRawText) || nodeNameIndexMap.contains("wm/concept/causal_factor/interventions/"+queryRawText))
    if (nodeQueryReformatMap.contains(queryRawText)) { wmConcepts.concepts(nodeNameIndexMap(nodeQueryReformatMap(queryRawText)))}
    else {wmConcepts.concepts(nodeNameIndexMap("wm/concept/causal_factor/interventions/"+queryRawText))}
  }

  def ymlTextToConcept(ymlText: String, w2v:CompactWord2Vec):Concept = {
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
  val w2v: CompactWord2Vec = ConceptUtils.word2Vec
  val aligner = WeightedParentSimilarityAligner.fromConfig(w2v)

  // actual testing
  behavior of "Top down concept aligner BOW node embedding"

  it should "have an precision@10 above 0.5" in {
    // load candidate answer embeddings
    val conceptSeq = TestAlignerTopDownUtils.constructCandidateAnswerExampleEmbeddings(w2v, topDownSamplePairSeq)

    var precisionSeq = new ArrayBuffer[Float]()
    for (evalPair <- topDownSamplePairSeq) {
      val ymlConcept = TestAlignerTopDownUtils.ymlTextToConcept(evalPair._1, w2v)
      val top10 = aligner.topk(ymlConcept, conceptSeq, 10)
      precisionSeq.append(TestAlignerTopDownUtils.getPrecisionAtTen(top10, evalPair._2))
    }

    val precisionAt10 = precisionSeq.sum/precisionSeq.length

    precisionAt10>0.5 should be (true)
  }

  // actual testing
  behavior of "Top down concept aligner WM node embedding"

  it should "have an precision@10 above 0.5" in {
    // load candidate answer embeddings
    val conceptSeq = TestAlignerTopDownUtils.constructCandidateAnswerExampleEmbeddings(w2v, topDownSamplePairSeq)
    val nodeNameIndexMap = TestAlignerTopDownUtils.constructNodeEmbeddingMappingsFromWMNodeEmbeddings()

    println("="*20)
    nodeNameIndexMap.foreach{x => println(x._1)}
    println("="*20)

    var precisionSeq = new ArrayBuffer[Float]()
    for (evalPair <- topDownSamplePairSeq) {

      // TODO: Check if the evaluation queries are in the wm node list!
      //if (!nodeNameIndexMap.contains(evalPair._1)) {println("Houston we have a trouble!")}
      println(evalPair._1, nodeNameIndexMap.contains("wm/concept/causal_factor/interventions/"+evalPair._1))

      val ymlConcept = TestAlignerTopDownUtils.getQueryConcept(TestAlignerTopDownUtils.tdConcepts, nodeNameIndexMap, evalPair._1)
      //val ymlConcept = TestAlignerTopDownUtils.ymlTextToConcept(evalPair._1, w2v)


      val top10 = aligner.topk(ymlConcept, conceptSeq, 10)
      precisionSeq.append(TestAlignerTopDownUtils.getPrecisionAtTen(top10, evalPair._2))
    }

    val precisionAt10 = precisionSeq.sum/precisionSeq.length

    precisionAt10>0.5 should be (true)
  }
}
