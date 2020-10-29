package org.clulab.alignment.webapp.controllers.v1

import javax.inject._
import org.clulab.alignment.webapp.controllers.utils.Busy
import org.clulab.alignment.webapp.controllers.utils.ExternalProcess
import org.clulab.alignment.webapp.controllers.utils.Ready
import org.clulab.alignment.webapp.controllers.utils.{Status => LocalStatus}
import org.clulab.alignment.webapp.controllers.utils.StatusHolder
import org.clulab.alignment.indexer.knn.hnswlib.HnswlibIndexer
import org.clulab.alignment.indexer.lucene.LuceneIndexerApp
import org.clulab.alignment.scraper.SuperMaasScraper
import org.clulab.alignment.scraper.SuperMaasScraperApp
import org.clulab.alignment.searcher.lucene.document.DatamartDocument
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue
import play.api.mvc.AbstractController
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import play.api.mvc.Request

import scala.concurrent.Future

class Indexer {
  val filename = "../datamarts.tsv"

  def run(): Unit = {
    println("Indexer started")
    new SuperMaasScraperApp(filename).run()
    new HnswlibIndexer().indexDatamart(filename)
    new LuceneIndexerApp(filename).run()
    // Do lots of copying to get the files in the right place?
    println("Indexer stopped")
  }
}

class IndexerManager(logger: Logger) {
  protected val statusHolder: StatusHolder = new StatusHolder(logger, Ready)

  def getStatus: LocalStatus = {
    statusHolder.get
  }

  def run(): LocalStatus = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val newStatus = {
      val status = getStatus

      if (status == Ready) {
        statusHolder.set(Busy)
        Future {
          new Indexer().run()
          statusHolder.set(Ready)
        }
        Busy
      }
      else
        status
    }

    newStatus
  }
}

class WebappManager(logger: Logger) extends ExternalProcess(Seq("cmd.exe", "/C", "startWebapp.bat")) {
  protected val statusHolder: StatusHolder = new StatusHolder(logger, Ready)
  protected var processOpt: Option[Process] = None

//  environment("_JAVA_OPTIONS", "-Xmx12g")
//  directory(".")

  def getStatus: LocalStatus = synchronized {
    statusHolder.get
  }

  def start(): LocalStatus = synchronized {
    val newStatus = {
      val status = getStatus

      if (status == Ready) {
        processOpt = Some(execute())
        // Only set if the above did not throw an exception.
        statusHolder.set(Busy)
        Busy
      }
      else
        status
    }

    newStatus
  }

  // This is not working on Windows.
  def stop(): LocalStatus = synchronized {
    val newStatus = {
      val status = getStatus

      if (status == Busy) {
        val newProcess = processOpt.get.destroyForcibly()
        val exitValue = newProcess.exitValue()
        if (!newProcess.isAlive) {
          statusHolder.set(Ready)
          processOpt = None
          Ready
        }
        else {
          processOpt = Some(newProcess)
          Busy
        }
      }
      else
        status
    }

    newStatus
  }
}

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  import HomeController.logger

  val webappManager = new WebappManager(logger)
  val indexerManager = new IndexerManager(logger)

  def home(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
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

  protected def getStatus(webappStatus: LocalStatus, indexerStatus: LocalStatus): JsValue = {
    val status = JsObject(Seq(
      "webapp" -> webappStatus.toJsValue,
      "indexer" -> indexerStatus.toJsValue
    ))

    status
  }

  protected def getStatus: JsValue =
      getStatus(webappManager.getStatus, indexerManager.getStatus)

  protected def getStatusWithWebapp(webappStatus: LocalStatus): JsValue =
      getStatus(webappStatus, indexerManager.getStatus)

  protected def getStatusWithIndexer(indexerStatus: LocalStatus): JsValue =
      getStatus(webappManager.getStatus, indexerStatus)

  def status(): Action[AnyContent] = Action {
    logger.info(s"Called 'status' function!")
    Ok(getStatus)
  }

  def start(): Action[AnyContent] = Action {
    logger.info(s"Called 'start' function!")
    val webappStatus = webappManager.start()
    Ok(getStatusWithWebapp(webappStatus))
  }

  def stop(): Action[AnyContent] = Action {
    logger.info(s"Called 'stop' function!")
    val webappStatus = webappManager.stop()
    Ok(getStatusWithWebapp(webappStatus))
  }

  def index(): Action[AnyContent] = Action {
    logger.info(s"Called 'index' function!")
    val indexerStatus = indexerManager.run()
    Ok(getStatusWithIndexer(indexerStatus))
  }
}

object HomeController {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
