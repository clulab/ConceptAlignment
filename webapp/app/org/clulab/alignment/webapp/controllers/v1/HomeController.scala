package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.webapp.AutoSearcher
import org.clulab.alignment.webapp.IndexMessage
import org.clulab.alignment.webapp.IndexReceiver
import org.clulab.alignment.webapp.IndexSender
import org.clulab.alignment.webapp.Searcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc._

@Singleton
class HomeController @Inject()(controllerComponents: ControllerComponents, prevSingleKnnAppFuture: AutoSearcher)
    extends AbstractController(controllerComponents) with IndexReceiver {
  import HomeController.logger

//  (indexSender: IndexSender) => receive(indexSender, new IndexMessage("hello"))
//  )

  val maxMaxHits = 500
  var currentSingleKnnAppFuture: Searcher = prevSingleKnnAppFuture
  // This one provides the double buffering.  Only one is provided, first come, first served.
  var nextSingleKnnAppFutureOpt: Option[Searcher] = None

  def receive(reindexSender: IndexSender, reindexMessage: IndexMessage): Unit = {
    println(s"I received the message ${reindexMessage.message}")
    currentSingleKnnAppFuture = nextSingleKnnAppFutureOpt.get
    nextSingleKnnAppFutureOpt = None
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
    Ok(currentSingleKnnAppFuture.statusHolder.toJsValue)
  }

  protected def toJsObject(datamartDocument: DatamartDocument, score: Float): JsObject = {
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
    val hits = math.min(maxMaxHits, maxHits) // Cap it off at some reasonable amount.
    val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = currentSingleKnnAppFuture.run(query, hits)
    val jsObjects = datamartDocumentsAndScores.map { case (datamartDocument, score) =>
      toJsObject(datamartDocument, score)
    }
    val jsValue: JsValue = JsArray(jsObjects)

    Ok(jsValue)
  }

  def reindex(who: String, where: String): Action[AnyContent] = Action {
    logger.info("Called 'reindex' function!")
    nextSingleKnnAppFutureOpt = Some(new Searcher(currentSingleKnnAppFuture.locations.next))

    Ok(currentSingleKnnAppFuture.statusHolder.toJsValue)
  }
}

object HomeController {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
