package org.clulab.alignment.controllers.utils

import com.google.inject.AbstractModule

class StartModule extends AbstractModule {
  override def configure() = {
    println("Running StartModule.configure")
    bind(classOf[SingleKnnAppFuture]).asEagerSingleton()
  }
}
