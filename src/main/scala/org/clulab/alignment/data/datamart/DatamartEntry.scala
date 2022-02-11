package org.clulab.alignment.data.datamart

case class DatamartEntry(identifier: DatamartIdentifier, words: Array[String], datasetDescription: Array[String], datasetTags: Array[String], variableDescription: Array[String], variableTags: Array[String])
