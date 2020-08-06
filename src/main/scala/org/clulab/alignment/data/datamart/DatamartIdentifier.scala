package org.clulab.alignment.data.datamart

import org.clulab.alignment.utils.Identifier

@SerialVersionUID(1L)
case class DatamartIdentifier(datamartId: String, datasetId: String, variableId: String) extends Identifier {

  override def toString(): String = s"$datamartId/$datasetId/$variableId"
}
