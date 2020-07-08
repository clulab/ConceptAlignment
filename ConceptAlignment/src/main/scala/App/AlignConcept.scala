import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import ai.lum.common.ConfigUtils._

import scala.io.BufferedSource
import scala.io.Source
import org.clulab.alignment._
import org.clulab.struct.Interval
import org.clulab.wm.eidos.{EidosProcessor, EidosSystem}
import org.clulab.wm.eidos.groundings.FullTreeDomainOntology.FullTreeDomainOntologyBuilder
import org.clulab.wm.eidos.groundings.OntologyAliases.OntologyGroundings
import org.clulab.wm.eidos.groundings.OntologyHandler
import org.clulab.wm.eidos.utils.{Canonicalizer, StopwordManager}

import scala.collection.mutable.ArrayBuffer


object AlignConcept extends App {

  // First, load the csv spread sheet
  val conceptAlignmentEvaluationSheet = AlignConcpetUtils.readEvaluationData()

  // Second, load the ontology nodes.
  val config = EidosSystem.defaultConfig
  val sentenceExtractor  = EidosProcessor("english", cutoff = 150)
  val tagSet = sentenceExtractor.getTagSet
  val stopwordManager = StopwordManager.fromConfig(config, tagSet)
  val ontologyHandler = OntologyHandler.load(config[Config]("ontologies"), sentenceExtractor, stopwordManager, tagSet)

  println("ontology handler loaded")

  // Third, write the actual code to align the concepts
  var correctCount = 0
  for (i <- conceptAlignmentEvaluationSheet.indices) {
    val intervention = conceptAlignmentEvaluationSheet(i)._1
    val interventioMappedOntology = conceptAlignmentEvaluationSheet(i)._2
    val exactMatch = conceptAlignmentEvaluationSheet(i)._3

    val interval = Interval(0, intervention.length)
    // Make a Document out of the sentence.
    // Get all groundings for the entity.
    val allGroundings: OntologyGroundings = ontologyHandler.reground(intervention, interval)
    val top5FlatGroundings = AlignConcpetUtils.getTop5(allGroundings)

    val filteredGroundings = top5FlatGroundings.filter( _.contains(interventioMappedOntology))
    if (filteredGroundings.nonEmpty){
      correctCount+=1
    }
  }
  println(s"top 5 matched nodes contain gold:${correctCount}/${conceptAlignmentEvaluationSheet.length}")


}

object AlignConcpetUtils {
  val utf8: String = StandardCharsets.UTF_8.toString

  def newFileNotFoundException(path: String): FileNotFoundException = {
    val message1 = path + " (The system cannot find the path specified"
    val message2 = message1 + (if (path.startsWith("~")) ".  Make sure to not use the tilde (~) character in paths in lieu of the home directory." else "")
    val message3 = message2 + ")"

    new FileNotFoundException(message3)
  }

  def sourceFromResource(path: String): BufferedSource = {
    val url = Option(this.getClass.getResource(path))
      .getOrElse(throw newFileNotFoundException(path))

    Source.fromURL(url, utf8)
  }

  def readEvaluationData():Seq[(String, String, Int)] = {
    val conceptAlignmentEvaluationSheet = ArrayBuffer[(String, String, Int)]()

    //TODO: read this from resource later
    val spreadsheetPath = "/Users/zhengzhongliang/NLP_Research/2020_WorldModeler/ConceptAlignment/src/main/scala/App/InterventionTaxonomyMapEvaluation.tsv"

    //val bufferedSource = sourceFromResource(spreadsheetPath)
    val bufferedSource = Source.fromFile(spreadsheetPath, utf8)
    for (line <- bufferedSource.getLines.drop(1)) {

      val cols = line.split("\t").map(_.trim)
      // do whatever you want with the columns here
      val intervention = cols(0)
      val interventionMappedOntology = cols(1)
      val exactMatchFlag = cols(2).toInt

      conceptAlignmentEvaluationSheet.append((intervention, interventionMappedOntology, exactMatchFlag))
    }

    conceptAlignmentEvaluationSheet.toSeq
  }

  def getTop5(allGroundings: OntologyGroundings, grounderName:String ="wm_flattened"): Seq[String] =
  allGroundings(grounderName)
    .take(5)
    .map(_._1.name)

}
