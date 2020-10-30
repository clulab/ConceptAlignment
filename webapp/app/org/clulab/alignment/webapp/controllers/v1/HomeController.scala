package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
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
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc._

@Singleton
class HomeController @Inject()(controllerComponents: ControllerComponents, prevIndexer: AutoIndexer, prevSearcher: AutoSearcher)
    extends AbstractController(controllerComponents) with IndexReceiver {
  import HomeController.logger

  // Get prevIndexer
  // They both probably need the index to use
  // Get these from environment variables

  val secrets: Array[String] = Option(System.getenv(HomeController.secretsKey))
      .map { secret =>
        secret.split('|')
      }
      .getOrElse(Array.empty)
  var currentIndexer: Indexer = prevIndexer
  var currentSearcher: Searcher = prevSearcher
  // These provides the double buffering.  Only one is provided, first come, first served.
  var nextIndexer: Option[Indexer] = None
  var nextSearcher: Option[Searcher] = None

  def receive(indexSender: IndexSender, indexMessage: IndexMessage): Unit = {
    println(s"I received the message ${indexMessage.message}")
    currentSearcher = nextSearcher.get
    nextSearcher = None
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
    Ok(currentSearcher.statusHolder.toJsValue)
  }

  protected def toJsObject(datamartDocumentsAndScores: (DatamartDocument, Float)): JsObject = {
    val (datamartDocument, score) = datamartDocumentsAndScores
    Json.obj(
      "score" -> score,
      "datamartId" -> datamartDocument.datamartId,
      "datasetId" -> datamartDocument.datasetId,
      "variableId" -> datamartDocument.variableId,
      "variableName" -> datamartDocument.variableName,
      "variableDescription" -> datamartDocument.variableDescription
    )
  }

  def search(query: String, maxHits: Int): Action[AnyContent] = Action {
    logger.info(s"Called 'search' function with '$query' and '$maxHits'!")
    val status = currentSearcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val hits = math.min(HomeController.maxMaxHits, maxHits)
      val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = currentSearcher.run(query, hits)
      val jsObjects = datamartDocumentsAndScores.map(toJsObject)
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  def reindex(secret: String): Action[AnyContent] = Action {
    logger.info("Called 'reindex' function with secret!")
    val status = currentIndexer.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    else if (status == IndexerStatus.Failing)
      InternalServerError
    else if (status == IndexerStatus.Loading)
      ServiceUnavailable
    else if (status == IndexerStatus.Indexing)
      ServiceUnavailable
    else {
      currentIndexer.run(Some(this))
      Ok
    }
  }
}

object HomeController {
  val secretsKey = "secrets"
  val maxMaxHits = 500 // Cap it off at some reasonable amount.
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
