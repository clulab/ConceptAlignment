package org.clulab.alignment.webapp

import com.google.inject.AbstractModule
import org.clulab.alignment.webapp.searcher.AutoSearchLocations
import org.clulab.alignment.webapp.searcher.AutoSearcher

class StartModule extends AbstractModule {
  override def configure() = {
    bind(classOf[AutoSearchLocations])
    bind(classOf[AutoSearcher]).asEagerSingleton()
  }
}
