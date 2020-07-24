package org.clulab.alignment.groundings

import org.clulab.wm.eidos.utils.Namer

class DatamartNamer(datamartId: String, datasetId: String, variableId: String) extends Namer {

  override def name: String = s"$datamartId/$datasetId/$variableId"

  def branch: Option[String] = None
}
