package controllers

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import play.api.mvc.Action
import org.clulab.alignment.SingleKnnApp
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.indexer.knn.hnswlib.index.DatamartIndex
import org.clulab.alignment.indexer.knn.hnswlib.item.DatamartAlignmentItem

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
//  val singleKnnApp = new SingleKnnApp()

  val datamartAlignmentItem = DatamartAlignmentItem(
    DatamartIdentifier("adsf", "asdf", "asdf"),
    Array(0f, 1f, 2f, 3f)
  )

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }


  def serialize(datamartIdentifier: DatamartIdentifier): Array[Byte] = {
    val bo = new ByteArrayOutputStream()
    val so = new ObjectOutputStream(bo)
    so.writeObject(datamartIdentifier)
    so.flush()
    val ba = bo.toByteArray

    ba
  }

  // Want to put this into file?
  def deserialize(bytes: Array[Byte]): DatamartIdentifier = {
    val b = bytes
    val bi = new ByteArrayInputStream(b)
    val si = new ObjectInputStream(bi)
    val obj = si.readObject().asInstanceOf[DatamartIdentifier]

    obj
  }

  def search(query: String, maxHits: Int) = Action {
    val datamartIdentifier = DatamartIdentifier("adsf", "asdf", "asdf")
    println("before serialization")
    val before = serialize(datamartIdentifier)
    println(before)
    println("before deserialization")
    val after = deserialize(before)
    println("after deserialization")

    val current = new java.io.File("../hnswlib-datamart.idx").getCanonicalPath()
    println("Current dir:"+current)
    val datamartIndex = DatamartIndex.load("../hnswlib-datamart.idx")
    println("Tried to load locally")

    val datamartDocumentsAndScores = new SingleKnnApp().run(query, maxHits)
    val result: JsValue = JsString(query)
//    datamartDocumentsAndScores.foreach { case (datamartDocument, score) =>
//      println(s"$datamartDocument.datamartId\t$datamartDocument.datasetId\t$datamartDocument.variableId\t$datamartDocument.variableDescription\t$score")
//    }
    Ok(result)
  }
}
