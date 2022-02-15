package org.clulab.alignment.webapp.controllers.v1

import com.typesafe.config.{Config, ConfigFactory}

import javax.inject._
import org.clulab.alignment.data.ontology.CompositionalOntologyIdentifier
import org.clulab.alignment.exception.{ExternalException, InternalException}
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.webapp.grounder.{IndicatorDocument, ModelDocument}
import org.clulab.alignment.webapp.indexer.AutoIndexer
import org.clulab.alignment.webapp.indexer.IndexMessage
import org.clulab.alignment.webapp.indexer.IndexReceiver
import org.clulab.alignment.webapp.indexer.IndexSender
import org.clulab.alignment.webapp.indexer.IndexerStatus
import org.clulab.alignment.webapp.indexer.IndexerTrait
import org.clulab.alignment.webapp.searcher.{AutoSearcher, CompositionalSearchSpec, Searcher, SearcherStatus}
import org.clulab.alignment.webapp.utils.OntologyVersion
import org.clulab.wm.wmexchanger2.wmconsumer.RealRestOntologyConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.http.MimeTypes
import play.api.libs.json.JsArray
import play.api.libs.json.JsLookupResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc._

import scala.util.Try

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

  protected def log(method: String, message: String = ""): Unit = {
    val sep = if (message.nonEmpty) " " else ""

    logger.info(s"Called '$method' function$sep$message!")
  }

  protected def getCatcher(method: String): PartialFunction[Throwable, Result] = {
    val catcher: PartialFunction[Throwable, Result] = {
      case throwable: Throwable =>
        logger.error(s"Caught exception in '$method'!", throwable)
        throwable match {
          case _: InternalException =>
            InternalServerError
          case exception: ExternalException =>
            BadRequest(exception.getMessage)
          case _: Throwable =>
            InternalServerError
        }
    }

    catcher
  }

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
    val method = "index"
    try {
      log(method)
      Ok(views.html.index())
    }
    catch getCatcher(method)
  }

  def ping: Action[AnyContent] = Action {
    val method = "ping"
    try {
      log(method)
      Ok
    }
    catch getCatcher(method)
  }

  def echo(text: String): Action[AnyContent] = Action {
    val method = "echo"
    try {
      log(method, s"with text='$text'")
      Ok(text)
    }
    catch getCatcher(method)
  }

  // This only makes sense if the indexer actually used this version.
  protected def getOntologyVersions: (String, String) = {
    val compVersion = OntologyVersion.get("/org/clulab/wm/eidos/english/ontologies/CompositionalOntology_metadata.properties")
    val flatVersion = OntologyVersion.get("/org/clulab/wm/eidos/english/ontologies/wm_flat_metadata.properties")

    (compVersion, flatVersion)
  }

  def status: Action[AnyContent] = Action {
    val method = "status"
    try {
      log(method)
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
    catch getCatcher(method)
  }

  def status2(ontologyIdOpt: Option[String]): Action[AnyContent] = Action {
    val method = "status2"
    try {
      log(method, s"with ontologyIdOpt='$ontologyIdOpt'")
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
    catch getCatcher(method)
  }

  def search(query: String, maxHits: Int, thresholdOpt: Option[Float]): Action[AnyContent] = Action {
    val method = "search"
    try {
      log(method, s"with '$query' and maxHits='$maxHits' and thresholdOpt='$thresholdOpt'")
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
    catch getCatcher(method)
  }

  def compositionalSearch(maxHits: Int, thresholdOpt: Option[Float]): Action[AnyContent] = Action { request =>
    val method = "compositionalSearch"
    try {
      val body: AnyContent = request.body
      log(method, s"with maxHits='$maxHits', thresholdOpt='$thresholdOpt', and body='$body'")
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        val jsonBodyOpt: Option[JsValue] = body.asJson
        val jsonBody = jsonBodyOpt.getOrElse(throw new ExternalException("A json body was expected."))
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
        // val compositionalOntologyToDatamarts = searcher.run(homeId, awayIds, hits, thresholdOpt)
        val compositionalOntologyToDocuments = searcher.run(homeId, awayIds, hits, thresholdOpt)
        val jsObjects = compositionalOntologyToDocuments.resultsToJsArray()

        Ok(jsObjects)
      }
    }
    catch getCatcher(method)
  }

  protected def getCompositionalSearchSpec(jsValue: JsValue): CompositionalSearchSpec = {
    val contextOpt = (jsValue \ "context").asOpt[String]
    val jsonHomeId = (jsValue \ "homeId").get
    val homeId = CompositionalOntologyIdentifier.fromJsValue(jsonHomeId)
    val awayIdsLookupResult: JsLookupResult = jsValue \ "awayIds"
    val awayIds =
        if (awayIdsLookupResult.isEmpty)
          Array.empty[CompositionalOntologyIdentifier]
        else {
          val jsonAwayIds = awayIdsLookupResult.get.asInstanceOf[JsArray]
          jsonAwayIds.value.map { jsValue =>
            CompositionalOntologyIdentifier.fromJsValue(jsValue)
          }.toArray
        }

    CompositionalSearchSpec(contextOpt, homeId, awayIds)
  }

  def compositionalSearch2(maxHits: Int, thresholdOpt: Option[Float], ontologyIdOpt: Option[String],
      geography: List[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Action[AnyContent] = Action { request =>
    val method = "compositionalSearch2"
    try {
      val body: AnyContent = request.body
      log(method, s"with maxHits='$maxHits', thresholdOpt='$thresholdOpt', ontologyIdOpt='$ontologyIdOpt', geography='${geographyToString(geography)}', periodGteOpt='$periodGteOpt', periodLteOpt='$periodLteOpt', and body='$body'")
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else if (periodGteOpt.isDefined && periodLteOpt.isDefined && periodGteOpt.get > periodLteOpt.get)
        BadRequest
      else {
        val jsonBodyOpt: Option[JsValue] = body.asJson
        val jsonBody = jsonBodyOpt.getOrElse(throw new ExternalException("A json body was expected."))
        val compositionalSearchSpec =
            try {
              getCompositionalSearchSpec(jsonBody)
            }
            catch {
              case throwable: Throwable => throw new ExternalException("Couldn't parse input.", throwable)
            }
        val hits = math.min(HomeController.maxMaxHits, maxHits)
        val compositionalOntologyToDocuments = searcher.run2(compositionalSearchSpec, hits, thresholdOpt,
            // TODO: ontologyIdOpt
            ontologyIdOpt, geography, periodGteOpt, periodLteOpt)
        val jsArray = compositionalOntologyToDocuments.resultsToJsArray()

        Ok(jsArray)
      }
    }
    catch getCatcher(method)
  }

  protected def geographyToString(geography: List[String]): String = geography.mkString("[", ", ", "]")

  def bulkCompositionalSearch2(secret: String, maxHitsOpt: Option[Int], thresholdOpt: Option[Float], ontologyIdOpt: Option[String],
      geography: List[String], periodGteOpt: Option[Long], periodLteOpt: Option[Long]): Action[AnyContent] = Action { request =>
    val method = "bulkCompositionalSearch2"
    try {
      val maxHits = maxHitsOpt.get
      val body: AnyContent = request.body
      log(method,s"with secret, maxHits='$maxHits', thresholdOpt='$thresholdOpt', ontologyIdOpt='$ontologyIdOpt', geography='${geographyToString(geography)}', periodGteOpt='$periodGteOpt', periodLteOpt='$periodLteOpt', and body='$body'")
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (false) // !secrets.contains(secret)) // TODO
        Unauthorized
      else if (status == SearcherStatus.Failing)
        InternalServerError
//      else if (searcher.compositionalOntologyMapperOpt.isEmpty)
//        ServiceUnavailable
      else {
        val jsonBodyOpt: Option[JsValue] = body.asJson
        val jsonBody = jsonBodyOpt.getOrElse(throw new ExternalException("A json body was expected."))
        val compositionalSearchSpecs =
            try {
              val jsArray = jsonBody.asInstanceOf[JsArray]

              jsArray.value.map { jsValue =>
                getCompositionalSearchSpec(jsValue)
              }.toArray // TODO
            }
            catch {
              case throwable: Throwable => throw new ExternalException("Couldn't parse input.", throwable)
            }
        val hits = math.min(HomeController.maxMaxHits, maxHits)

        val jsArray = if (false) {
          val multipleCompositionalOntologyToDocuments = searcher.run2(compositionalSearchSpecs, hits, thresholdOpt,
             // TODO: ontologyIdOpt
              ontologyIdOpt, geography, periodGteOpt, periodLteOpt)
          val jsObjects = multipleCompositionalOntologyToDocuments.map { compositionalOntologyToDocument =>
            compositionalOntologyToDocument.resultsToJsArray()
          }
          val jsArray = JsArray(jsObjects)
          jsArray
        }
        else {
          val compositionalOntologyToDocuments = searcher.run2(compositionalSearchSpecs.head, hits, thresholdOpt,
              // TODO: ontologyIdOpt
              ontologyIdOpt, geography, periodGteOpt, periodLteOpt)
          val jsArray = compositionalOntologyToDocuments.resultsToJsArray()
          JsArray(List(jsArray))
        }

        Ok(jsArray)
      }
    }
    catch getCatcher(method)
  }

  def bulkSearchOntologyToDatamart(secret: String, maxHitsOpt: Option[Int] = None, thresholdOpt: Option[Float]): Action[AnyContent] = Action {
    val method = "bulkSearchOntologyToDatamart"
    try {
      log(method,s"with secret, maxHits='$maxHitsOpt' and thresholdOpt='$thresholdOpt'")
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
    catch getCatcher(method)
  }

  def bulkSearchDatamartToOntology(secret: String, maxHitsOpt: Option[Int] = None, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action {
    val method = "bulkSearchDatamartToOntology"
    try {
      log(method,s"with secret, maxHits='$maxHitsOpt' and thresholdOpt='$thresholdOpt' and compositional='$compositional'")
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
    catch getCatcher(method)
  }

  def addOntology2(secret: String, ontologyId: String): Action[AnyContent] = Action {
    val method = "addOntology2"
    try {
      log(method,s"with secret and ontologyId=`$ontologyId`")
      // ontologyId = fd513e69-01fd-4b9a-a609-610ff0394ddb
      val config: Config = ConfigFactory.defaultApplication().resolve()
      val ontologyService: String = config.getString("rest.consumer.ontologyService")
      val username: String = Try(config.getString("rest.consumer.username")).getOrElse("eidos")
      val password: String = Try(config.getString("rest.consumer.password")).getOrElse("quick_OHIO_flat_94")

      val ontology = new RealRestOntologyConsumer(ontologyService, username, password).autoClose { restOntologyConsumer =>
        restOntologyConsumer.open()
        restOntologyConsumer.download(ontologyId)
      }
      println(ontology)
      Ok
    }
    catch getCatcher(method)
  }

  def reindex(secret: String): Action[AnyContent] = Action {
    val method = "reindex"
    try {
      log(method, "with secret")
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
    catch getCatcher(method)
  }

  def groundIndicator(maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action { request =>
    val method = "groundIndicator"
    try {
      val body: AnyContent = request.body
      log(method,s"with maxHits='$maxHits', thresholdOpt='$thresholdOpt', compositional='$compositional', and body='$body'")
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        body.asJson.getOrElse(throw new ExternalException("A json body was expected."))
        val dojoDocument = new IndicatorDocument(Json.stringify(body.asJson.get))
        // TODO: This is a very unfortunate conversion.
        val json = searcher.run(dojoDocument, maxHits, thresholdOpt, compositional)

        Ok(json).as(MimeTypes.JSON)
      }
    }
    catch getCatcher(method)
  }

  def groundModel(maxHits: Int, thresholdOpt: Option[Float], compositional: Boolean): Action[AnyContent] = Action { request =>
    def method = "groundModel"
    try {
      val body: AnyContent = request.body
      log(method,s"with maxHits='$maxHits', thresholdOpt='$thresholdOpt', compositional='$compositional', and body='$body'")
      val searcher = currentSearcher
      val status = searcher.getStatus
      if (status == SearcherStatus.Failing)
        InternalServerError
      else {
        body.asJson.getOrElse(throw new ExternalException("A json body was expected."))
        // TODO: This is a very unfortunate conversion.
        val dojoDocument = new ModelDocument(Json.stringify(body.asJson.get))
        val json = searcher.run(dojoDocument, maxHits, thresholdOpt, compositional)

        Ok(json).as(MimeTypes.JSON)
      }
    }
    catch getCatcher(method)
  }
}

object HomeController {
  val VERSION = "1.5.1"

  val secretsKey = "secrets"
  val maxMaxHits = 500 // Cap it off at some reasonable amount.
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
