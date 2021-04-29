package org.clulab.alignment.utils

import ai.lum.common.ConfigUtils._
import com.typesafe.config.Config
import org.clulab.wm.eidos.EidosSystem
import org.clulab.wm.eidos.groundings.OntologyHandler
import org.clulab.wm.eidos.utils.StopwordManager
import org.clulab.wm.eidoscommon.EidosProcessor

object OntologyHandlerHelper {

  def fromConfig(config: Config = EidosSystem.defaultConfig): OntologyHandler = {
    val sentenceExtractor = EidosProcessor("english", cutoff = 150)
    val tagSet = sentenceExtractor.getTagSet
    val stopwordManager = StopwordManager.fromConfig(config, tagSet)

    OntologyHandler.load(config[Config]("ontologies"), sentenceExtractor, stopwordManager, tagSet, sentenceExtractor.eidosTokenizer)
  }
}
