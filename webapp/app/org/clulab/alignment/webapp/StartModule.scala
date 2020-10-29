package org.clulab.alignment.webapp

import com.google.inject.AbstractModule

class StartModule extends AbstractModule {
  override def configure() = {
    bind(classOf[AutoKnnLocations])
    bind(classOf[AutoSingleKnnAppFuture]).asEagerSingleton()
  }
}
