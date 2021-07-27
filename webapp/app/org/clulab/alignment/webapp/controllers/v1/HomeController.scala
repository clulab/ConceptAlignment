package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.CompositionalOntologyToDatamarts
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.PropertiesBuilder
import org.clulab.alignment.webapp.grounder.{IndicatorDocument, ModelDocument}
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
import play.api.http.MimeTypes
import play.api.libs.json.JsArray
import play.api.libs.json.JsLookupResult
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
    logger.info(s"Called 'receive' function with index ${indexMessage.index}")
    // It will only get here if the reindexing was successful so that the new Searcher
    // is able to rely on the values coming from the message.
    val prevSearcher = currentSearcher
    currentSearcher = currentSearcher.next(indexMessage.index, indexMessage.datamartIndex)
    prevSearcher.close() // if it isn't busy, which is hard to know just now
    // TODO: Indexes related to the previous searcher could be deleted.
    // Could do it on finalization, but only if close indicated?
  }

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def ping: Action[AnyContent] = Action {
    logger.info("Called 'ping' function!")
    Ok
  }

  def echo(text: String): Action[AnyContent] = Action {
    logger.info(s"Called 'echo' function with text='$text'!")
    Ok(text)
  }

  // This only makes sense if the indexer actually used this version.
  protected def getOntologyVersions: (String, String) = {

    def getOntologyVersion(propertiesPath: String): String = {
      val properties = PropertiesBuilder.fromResource(propertiesPath).get
      val hash = Option(properties.getProperty("hash"))
      val ontologyVersion = hash.getOrElse("<unknown>")

      ontologyVersion
    }

    val compVersion = getOntologyVersion("/org/clulab/wm/eidos/english/ontologies/CompositionalOntology_metadata.properties")
    val flatVersion = getOntologyVersion("/org/clulab/wm/eidos/english/ontologies/wm_flat_metadata.properties")

    (compVersion, flatVersion)
  }

  def status: Action[AnyContent] = Action {
    logger.info("Called 'status' function!")
    val indexer = currentIndexer
    val searcher = currentSearcher
    val (compVersion, flatVersion) = getOntologyVersions
    val jsObject = Json.obj(
      "version" -> HomeController.VERSION,
      "compOntology" -> compVersion,
      "flatOntology" -> flatVersion,
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
    logger.info(s"Called 'search' function with '$query' and maxHits='$maxHits' and thresholdOpt='$thresholdOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else if (!searcher.isReady)
      ServiceUnavailable
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

  def compositionalSearch(maxHits: Int, thresholdOpt: Option[Float]): Action[AnyContent] = Action { request =>
    val body: AnyContent = request.body
    logger.info(s"Called 'compositionalSearch' function with '$body' and maxHits='$maxHits' and thresholdOpt='$thresholdOpt'!")
    try {
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        val jsonBodyOpt: Option[JsValue] = body.asJson
        val jsonBody = jsonBodyOpt.getOrElse(throw new RuntimeException("A json body was expected."))
        val jsonHomeId = (jsonBody \ "homeId").get
        val homeId = CompositionalOntologyIdentifier.fromJsValue(jsonHomeId)
        val awayIdsLookupResult: JsLookupResult = jsonBody \ "awayIds"
        val awayIds =
            if (awayIdsLookupResult.isEmpty)
              Array.empty[CompositionalOntologyIdentifier]
            else {
              val jsonAwayIds = awayIdsLookupResult.get.asInstanceOf[JsArray]
              jsonAwayIds.value.map { jsValue =>
                CompositionalOntologyIdentifier.fromJsValue(jsValue)
              }.toArray
            }
        val hits = math.min(HomeController.maxMaxHits, maxHits)
        val compositionalOntologyToDatamarts = searcher.run(homeId, awayIds, hits, thresholdOpt)
        val jsObjects = compositionalOntologyToDatamarts.resultsToJsArray()

        Ok(jsObjects)
      }
    }
    catch {
      case throwable: Throwable =>
        logger.error("An exception was thrown in compositionalSearch:", throwable)
        // Make believe that the problem is with the request.  Use the log message to diagnose.
        BadRequest
    }
  }

  def bulkSearchOntologyToDatamart(secret: String, maxHitsOpt: Option[Int] = None, thresholdOpt: Option[Float]): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearchOntologyToDatamart' function with maxHits='$maxHitsOpt' and thresholdOpt='$thresholdOpt'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    else if (status == SearcherStatus.Failing)
      InternalServerError
    else if (searcher.flatOntologyMapperOpt.isEmpty)
      ServiceUnavailable
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
    logger.info(s"Called 'bulkSearchDatamartToOntology' function with maxHits='$maxHitsOpt' and thresholdOpt='$thresholdOpt' and compositional='$compositional'!")
    val searcher = currentSearcher
    val status = searcher.getStatus
    if (!secrets.contains(secret))
      Unauthorized
    else if (status == SearcherStatus.Failing)
      InternalServerError
    else if (searcher.flatOntologyMapperOpt.isEmpty || searcher.compositionalOntologyMapperOpt.isEmpty)
      ServiceUnavailable
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

  def groundIndicator(maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action { request =>
    val body: AnyContent = request.body
    logger.info(s"Called 'groundIndicator' function  with '$body' and maxHits='$maxHits' and thresholdOpt='$thresholdOpt' and compositional='$compositional'!")
    try {
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        body.asJson.getOrElse(throw new RuntimeException("A json body was expected."))
        val dojoDocument = new IndicatorDocument(Json.stringify(body.asJson.get))
        // TODO: This is a very unfortunate conversion.
        val json = searcher.run(dojoDocument, maxHits, thresholdOpt, compositional)

        Ok(json).as(MimeTypes.JSON)
      }
    }
    catch {
      case throwable: Throwable =>
        logger.error("An exception was thrown in groundIndicator:", throwable)
        // Make believe that the problem is with the request.  Use the log message to diagnose.
        BadRequest
    }
  }

  def groundModel(maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action { request =>
    val body: AnyContent = request.body
    logger.info(s"Called 'groundModel' function with '$body' and maxHits='$maxHits' and thresholdOpt='$thresholdOpt' and compositional='$compositional'!")
    try {
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        body.asJson.getOrElse(throw new RuntimeException("A json body was expected."))
        // TODO: This is a very unfortunate conversion.
        val dojoDocument = new ModelDocument(Json.stringify(body.asJson.get))
        val json = searcher.run(dojoDocument, maxHits, thresholdOpt, compositional)

        Ok(json).as(MimeTypes.JSON)
      }
    }
    catch {
      case throwable: Throwable =>
        logger.error("An exception was thrown in groundModel:", throwable)
        // Make believe that the problem is with the request.  Use the log message to diagnose.
        BadRequest
    }
  }
}

object HomeController {
  val VERSION = "1.3.0"

  val secretsKey = "secrets"
  val maxMaxHits = 500 // Cap it off at some reasonable amount.
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
