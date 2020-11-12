package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.OntologyMapper
import org.clulab.alignment.OntologyMapperApp.OntologyToDatamart
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
  // todo
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

  protected def toJsObject(otd: OntologyToDatamart): JsObject = {
    val src = otd.srcId.toString()
    val dstItem = otd.dstResult.item()
    val score = otd.dstResult.distance()
    Json.obj(
      "ontologyNode" -> src,
      "datamartId" -> dstItem.id.datamartId,
      "datasetId" -> dstItem.id.datasetId,
      "variableId" -> dstItem.id.variableId,
      "score" -> score
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

  def bulkSearchOntologyToDatamart(secret: String, maxHits: Option[Int] = None): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearch' function with maxHits='$maxHits' and secret!")
    logger.info("renewing the datamart index")
    reindex(secret)
    logger.info("renewing the ontology index")
    // todo
    val datamartIndex = DatamartIndex.load(datamartFilename)
    val ontologyIndex = OntologyIndex.load(ontologyFilename)

    val mapper = new OntologyMapper(datamartIndex, ontologyIndex)

    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val ontToDatamart = mapper.ontologyToDatamartMapping().toArray
      val dmToOntology = mapper.datamartToOntologyMapping()
      val jsObjects = for {
        rankedAlignments <- ontToDatamart
        otd <- rankedAlignments
      } yield toJsObject(otd)
      val jsValue: JsValue = JsArray(jsObjects)

      Ok(jsValue)
    }
  }
   def bulkSearchOntologyToDatamart(secret: String, maxHits: Option[Int] = None): Action[AnyContent] = Action {
    logger.info(s"Called 'bulkSearch' function with maxHits='$maxHits' and secret!")
    logger.info("renewing the datamart index")
    reindex(secret)
    logger.info("renewing the ontology index")
    // todo
    val datamartIndex = DatamartIndex.load(datamartFilename)
    val ontologyIndex = OntologyIndex.load(ontologyFilename)

    val mapper = new OntologyMapper(datamartIndex, ontologyIndex)

    val status = searcher.getStatus
    if (status == SearcherStatus.Failing)
      InternalServerError
    else {
      val ontToDatamart = mapper.ontologyToDatamartMapping().toArray
      val dmToOntology = mapper.datamartToOntologyMapping()
      val jsObjects = for {
        rankedAlignments <- ontToDatamart
        otd <- rankedAlignments
      } yield toJsObject(otd)
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
