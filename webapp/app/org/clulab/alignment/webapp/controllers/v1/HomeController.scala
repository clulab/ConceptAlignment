package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.CompositionalOntologyToDatamarts
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.webapp.indexer.AutoIndexer
import org.clulab.alignment.webapp.indexer.IndexMessage
import org.clulab.alignment.webapp.indexer.IndexReceiver
import org.clulab.alignment.webapp.indexer.IndexSender
import org.clulab.alignment.webapp.indexer.IndexerStatus
import org.clulab.alignment.webapp.indexer.IndexerTrait
import org.clulab.alignment.webapp.searcher.AutoSearcher
import org.clulab.alignment.webapp.searcher.Searcher
import org.clulab.alignment.webapp.searcher.SearcherStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc._

@Singleton
class HomeController @Inject()(controllerComponents: ControllerComponents, prevIndexer: AutoIndexer, prevSearcher: AutoSearcher)
    extends AbstractController(controllerComponents) with IndexReceiver {
  import HomeController.logger

  var currentIndexer: IndexerTrait = prevIndexer
  var currentSearcher: Searcher = prevSearcher
  val secrets: Array[String] = Option(System.getenv(HomeController.secretsKey))
      .map { secret =>
        secret.split('|')
      }
      .getOrElse(Array.empty)
  val datamartFilename = "../hnswlib-datamart.idx"
  val ontologyFilename = "../hnswlib-wm_flattened.idx"

  def receive(indexSender: IndexSender, indexMessage: IndexMessage): Unit = {
    println(s"Called 'receive' function")
    // It will only get here if the reindexing was successful so that the new Searcher
    // is able to rely on the values coming from the message.
    val prevSearcher = currentSearcher
    currentSearcher = currentSearcher.next(indexMessage.index, indexMessage.datamartIndex)
    prevSearcher.close() // if it isn't busy, which is hard to know just now
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

  def search(query: String, maxHits: Int, thresholdOpt: Option[Float]): Action[AnyContent] = Action {
    logger.info(s"Called 'search' function with '$query' and '$maxHits' and '$thresholdOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val hits = math.min(HomeController.maxMaxHits, maxHits)
      val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = searcher.run(query, hits, thresholdOpt)
      val jsObjects = datamartDocumentsAndScores.map { case (datamartDocument, score) =>
        datamartDocument.toJsObject(score)
      }
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  def compositionalSearch(maxHits: Int, threshold: Option[Float]): Action[AnyContent] = Action { request =>
    val body: AnyContent = request.body
    val maxHits = 10
    val threshold = Some(1f)

    logger.info(s"Called 'compositionalSearch' function with '$body' and maxHits='$maxHits' and '$threshold'!")
//homeIdJson: String, awayIdsJson: Option[String],
    try {
      val jsonBodyOpt: Option[JsValue] = body.asJson
      val jsonBody = jsonBodyOpt.getOrElse(throw new RuntimeException("A json body was expected."))


      //homeId: String, awayIds: Option[String],
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        val hits = math.min(HomeController.maxMaxHits, maxHits)
        val homeId: CompositionalOntologyIdentifier = null
        val awayIds: Array[CompositionalOntologyIdentifier] = Array.empty

        // TODO throw an exception?
        val compositionalOntologyToDatamartsOpt: Option[CompositionalOntologyToDatamarts] =
          searcher.compositionalOntologyMapperOpt.get.ontologyItemToDatamartMapping(homeId, awayIds, Some(hits), threshold)
        val jsObjectsOpt = compositionalOntologyToDatamartsOpt.map { compositionalOntologyToDatamarts =>
          compositionalOntologyToDatamarts.toJsObject
        }
        val jsValue: JsValue = jsObjectsOpt.getOrElse(JsString("Hello"))

        Ok(jsValue)
      }
    }
    catch {
      case _: Throwable => BadRequest
    }
  }

  def bulkSearchOntologyToDatamart(secret: String, maxHitsOpt: Option[Int] = None, thresholdOpt: Option[Float]): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearchOntologyToDatamart' function with maxHits='$maxHitsOpt' and '$thresholdOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    else if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val jsObjects = {
        val allOntologyToDatamarts = searcher.flatOntologyMapperOpt.get.ontologyToDatamartMapping(maxHitsOpt, thresholdOpt)
        allOntologyToDatamarts.map(_.toJsObject).toSeq
      }
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }

  def bulkSearchDatamartToOntology(secret: String, maxHitsOpt: Option[Int] = None, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearchDatamartToOntology' function with maxHits='$maxHitsOpt' and '$thresholdOpt' and '$compositional'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    else if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val jsObjects =
          if (!compositional) {
            val allDatamartToOntologies = searcher.flatOntologyMapperOpt.get.datamartToOntologyMapping(maxHitsOpt, thresholdOpt)
            allDatamartToOntologies.map(_.toJsObject).toSeq
          }
          else {
            val allDatamartToOntologies = searcher.compositionalOntologyMapperOpt.get.datamartToOntologyMapping(maxHitsOpt, thresholdOpt)
            allDatamartToOntologies.map(_.toJsObject)
          }
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
    else if (status == IndexerStatus.Crashing)
      InternalServerError
    // Allow retries by ignoring this state.
    // else if (status == IndexerStatus.Failing)
    //   InternalServerError
    else if (status == IndexerStatus.Loading)
      ServiceUnavailable
    else if (status == IndexerStatus.Indexing)
      ServiceUnavailable
    else { // likely Idling
      // Do not set the currentSearcher yet because it could fail.
      // Do set the current indexer so that upon fail, a different
      // one can be used with the next index.
      currentIndexer = indexer.next(Some(this))
      Created
    }
  }
}

object HomeController {
  val secretsKey = "secrets"
  val maxMaxHits = 500 // Cap it off at some reasonable amount.
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
