package org.clulab.alignment.controllers.utils

import scala.concurrent.Future
import javax.inject._
import play.api.inject.ApplicationLifecycle

class ApplicationStart { // @Inject()(lifecycle: ApplicationLifecycle) {
  // This would be the expensive thing to create.  But it could also be a future.
  // Create this right away before the web page is accessed.
  println("ApplicationStart constructor was called")
}
