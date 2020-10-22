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

  println("Configuring...")

  println("Initializing...")

  println("Up and running...")

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

  def status(): Action[AnyContent] = Action {
    logger.info(s"Called 'status' function!")
    Ok
  }

  def start(): Action[AnyContent] = Action {
    logger.info(s"Called 'status' function!")
    Ok
  }

  def stop(): Action[AnyContent] = Action {
    logger.info(s"Called 'status' function!")
    Ok
  }

  def index(): Action[AnyContent] = Action {
    logger.info(s"Called 'index' function!")
    Ok
  }
}

object HomeController {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
}
