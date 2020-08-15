package controllers

import javax.inject._

import org.clulab.alignment.Locations
import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Action

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

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

  def search(query: String, maxHits: Int): Action[AnyContent] = Action {
    val datamartDocumentsAndScores: Seq[(DatamartDocument, Float)] = singleKnnApp.run(query, maxHits)
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
