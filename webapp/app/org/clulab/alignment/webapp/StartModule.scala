package org.clulab.alignment.webapp

import com.google.inject.AbstractModule
import org.clulab.alignment.webapp.indexer.AutoIndexer
import org.clulab.alignment.webapp.searcher.AutoSearcher
import org.clulab.alignment.webapp.utils.AutoLocations

class StartModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AutoLocations]).asEagerSingleton()
    bind(classOf[AutoIndexer]).asEagerSingleton()
    bind(classOf[AutoSearcher]).asEagerSingleton()
  }
}
