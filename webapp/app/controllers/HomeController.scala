package controllers

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Action
import org.clulab.alignment.SingleKnnApp

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
//  val singleKnnApp = new SingleKnnApp()

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def search(query: String, maxHits: Int) = Action {
    val current = new java.io.File( "../hnswlib-datamart.idx" ).getCanonicalPath()
    println("Current dir:"+current)

    val datamartDocumentsAndScores = new SingleKnnApp().run(query, maxHits)
    val result: JsValue = JsString(query)
//    datamartDocumentsAndScores.foreach { case (datamartDocument, score) =>
//      println(s"$datamartDocument.datamartId\t$datamartDocument.datasetId\t$datamartDocument.variableId\t$datamartDocument.variableDescription\t$score")
//    }
    Ok(result)
  }
}
