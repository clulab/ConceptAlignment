package org.clulab.alignment.controllers.v1

import javax.inject._

import org.clulab.alignment.Locations
import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Action

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  import HomeController.logger

  val maxMaxHits = 500

  def configure(filename: String): Unit = {
    val canonicalPath = new java.io.File(filename).getCanonicalPath
    println("Place file here: " + canonicalPath)
  }

  println("Configuring...")
  configure(Locations.datamartFilename)
  configure(Locations.luceneDirname)
  configure(Locations.gloveFilename)

  println("Initializing...")
  val singleKnnApp = new SingleKnnApp()

  println("Up and running...")

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

  def search(query: String, maxHits: Int): Action[AnyContent] = Action {
    logger.info(s"Called 'search' function with '$query' and '$maxHits'!")
    val hits = math.min(maxMaxHits, maxHits) // Cap it off at some reasonable amount.
    val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = singleKnnApp.run(query, hits)
    val jsObjects = datamartDocumentsAndScores.map { case (datamartDocument, score) =>
      Json.obj(
      "score" -> score,
        "datamartId" -> datamartDocument.datamartId,
        "datasetId" -> datamartDocument.datasetId,
        "variableId" -> datamartDocument.variableId,
        "variableDescription" -> datamartDocument.variableDescription
      )
    }
    val jsValue: JsValue = JsArray(jsObjects)

    Ok(jsValue)
  }
}

object HomeController {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
