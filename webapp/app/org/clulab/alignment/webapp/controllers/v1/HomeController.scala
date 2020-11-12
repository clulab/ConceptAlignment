package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.OntologyMapper
import org.clulab.alignment.OntologyMapperApp.DatamartToOntologies
import org.clulab.alignment.OntologyMapperApp.OntologyToDatamarts
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.{DatamartIndex, OntologyIndex}
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.webapp.indexer.AutoIndexer
import org.clulab.alignment.webapp.indexer.IndexMessage
import org.clulab.alignment.webapp.indexer.IndexReceiver
import org.clulab.alignment.webapp.indexer.IndexSender
import org.clulab.alignment.webapp.indexer.Indexer
import org.clulab.alignment.webapp.indexer.IndexerStatus
import org.clulab.alignment.webapp.searcher.AutoSearcher
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.webapp.searcher.SearcherStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc._

@Singleton
class HomeController @Inject()(controllerComponents: ControllerComponents, prevIndexer: AutoIndexer, prevSearcher: AutoSearcher)
    extends AbstractController(controllerComponents) with IndexReceiver {
  import HomeController.logger

  var currentIndexer: Indexer = prevIndexer
  var currentSearcher: Searcher = prevSearcher
  // These provides the double buffering.  Only one is provided, first come, first served.
  var nextIndexer: Option[Indexer] = None
  var nextSearcher: Option[Searcher] = None
  val secrets: Array[String] = Option(System.getenv(HomeController.secretsKey))
      .map { secret =>
        secret.split('|')
      }
      .getOrElse(Array.empty)
  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-wm_flattened.idx"

  def receive(indexSender: IndexSender, indexMessage: IndexMessage): Unit = {
    println(s"Called 'receive' function")

    val prevIndexer = indexMessage.indexer
    val prevSearcher = currentSearcher

    val nextIndexer = prevIndexer.next
    val nextSearcher = currentSearcher.next(prevIndexer.index, indexMessage.datamartIndex)

    currentIndexer = nextIndexer
    currentSearcher = nextSearcher

    prevIndexer.close()
    prevSearcher.close()
  }

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def ping: Action[AnyContent] = Action {
    logger.info("Called 'ping' function!")
    Ok
  }

  def echo(text: String): Action[AnyContent] = Action {
    logger.info(s"Called 'echo' function with '$text'!")
    Ok(text)
  }

  def status: Action[AnyContent] = Action {
    logger.info("Called 'status' function!")
    val indexer = currentIndexer
    val searcher = currentSearcher
    val jsObject = Json.obj(
      "searcher" -> Json.obj(
        "index" -> searcher.index,
        "status" -> searcher.getStatus.toJsValue
      ),
      "indexer" -> Json.obj(
        "index" -> indexer.index,
        "status" -> indexer.getStatus.toJsValue
      )
    )
    Ok(jsObject)
  }

  protected def toJsObject(datamartDocumentsAndScore: (DatamartDocument, Float)): JsObject = {
    val (datamartDocument, score) = datamartDocumentsAndScore
    Json.obj(
      "score" -> safeScore(score),
      "datamartId" -> datamartDocument.datamartId,
      "datasetId" -> datamartDocument.datasetId,
      "variableId" -> datamartDocument.variableId,
      "variableName" -> datamartDocument.variableName,
      "variableDescription" -> datamartDocument.variableDescription
    )
  }

  protected def toJsObject(datamartIdentifier: DatamartIdentifier): JsObject = {
    Json.obj(
      "datamartId" -> datamartIdentifier.datamartId,
      "datasetId" -> datamartIdentifier.datasetId,
      "variableId" -> datamartIdentifier.variableId
    )
  }

  def search(query: String, maxHits: Int): Action[AnyContent] = Action {
    logger.info(s"Called 'search' function with '$query' and '$maxHits'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val hits = math.min(HomeController.maxMaxHits, maxHits)
      val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = searcher.run(query, hits)
      val jsObjects = datamartDocumentsAndScores.map(toJsObject)
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  // This could return an Option[Float] instead.  Alternatively, the values
  // with NaN could be filtered out from the answers.
  protected def safeScore(score: Float): Float = if (score.isNaN) 0f else score

  protected def toJsObject(ontologyToDatamarts: OntologyToDatamarts): JsObject = {
    val ontologyIdentifier = ontologyToDatamarts.srcId
    val searchResults = ontologyToDatamarts.dstResults.toArray
    val jsDatamartValues = searchResults.map { searchResult =>
      Json.obj(
        "score" -> safeScore(searchResult.distance),
        "datamart" -> toJsObject(searchResult.item.id)
      )
    }
    Json.obj(
      "ontology" -> ontologyIdentifier.nodeName,
      "datamarts" -> JsArray(jsDatamartValues)
    )
  }

  def bulkSearchOntologyToDatamart(maxHitsOpt: Option[Int] = None): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearchOntologyToDatamart' function with maxHits='$maxHitsOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val allOntologyToDatamarts = searcher.ontologyMapperOpt.get.ontologyToDatamartMapping(maxHitsOpt)
      val jsObjects = allOntologyToDatamarts.map(toJsObject).toArray
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  protected def toJsObject(datamartToOntologies: DatamartToOntologies): JsObject = {
    val datamartIdentifier = datamartToOntologies.srcId
    val searchResults = datamartToOntologies.dstResults.toArray
    val jsOntologyValues = searchResults.map { searchResult =>
      Json.obj(
        "score" -> safeScore(searchResult.distance),
        "ontology" -> JsString(searchResult.item.id.nodeName)
      )
    }
    Json.obj(
      "datamart" -> toJsObject(datamartIdentifier),
      "ontologies" -> JsArray(jsOntologyValues)
    )
  }

  def bulkSearchDatamartToOntology(maxHitsOpt: Option[Int] = None): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearchDatamartToOntology' function with maxHits='$maxHitsOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val allDatamartToOntologies = searcher.ontologyMapperOpt.get.datamartToOntologyMapping(maxHitsOpt)
      val jsObjects = allDatamartToOntologies.map(toJsObject).toArray
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  def reindex(secret: String): Action[AnyContent] = Action {
    logger.info("Called 'reindex' function with secret!")
    val indexer = currentIndexer
    val status = indexer.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    // Allow retries by ignoring this state.
    // else if (status == IndexerStatus.Failing)
    //   InternalServerError
    else if (status == IndexerStatus.Loading)
      ServiceUnavailable
    else if (status == IndexerStatus.Indexing)
      ServiceUnavailable
    else {
      indexer.run(Some(this))
      Created
    }
  }
}

object HomeController {
  val secretsKey = "secrets"
  val maxMaxHits = 500 // Cap it off at some reasonable amount.
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
