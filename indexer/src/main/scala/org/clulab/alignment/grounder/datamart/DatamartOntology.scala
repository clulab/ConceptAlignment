package org.clulab.alignment.grounder.datamart

import org.clulab.alignment.data.Tokenizer
import org.clulab.alignment.data.datamart.DatamartEntry
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.utils.Closer.AutoCloser
import org.clulab.alignment.utils.Sourcer
import org.clulab.alignment.utils.TsvReader

class DatamartOntology(val datamartEntries: Seq[DatamartEntry]) {

  def size: Int = datamartEntries.size
}

object DatamartOntology {

  def tagsToStrings(json: String, tokenizer: Tokenizer): Seq[String] = {
    val allTags = ujson.read(json).arr.toIndexedSeq.map(_.str)
    val niceTags = allTags.filter(!_.contains("::"))
    val tokenizedTags = niceTags.flatMap(tokenizer.tokenize)

    tokenizedTags
  }

  def fromFile(filename: String, tokenizer: Tokenizer): DatamartOntology = {
    val tsvReader = new TsvReader()
    val datamartEntries = Sourcer.sourceFromFile(filename).autoClose { source =>
      source.getLines.buffered.drop(1).map { line =>
          println(line)
        val Array(
          datamartId,
          datasetId,
          _, // dataset_name
          datasetTagsJson,
          datasetDescriptionS, // dataset_description
          _, // dataset_url
          variableId,
          _, // variable_name
          variableTagsJson,
          variableDescriptionS
        ) = tsvReader.readln(line, length = 10)
        val datamartIdentifier = new DatamartIdentifier(datamartId, datasetId, variableId)
        val datasetTags = tagsToStrings(datasetTagsJson, tokenizer).toArray
        val datasetDescription = tokenizer.tokenize(datasetDescriptionS)
        val variableTags = tagsToStrings(variableTagsJson, tokenizer).toArray
        val variableDescription = tokenizer.tokenize(variableDescriptionS)
        val words = tokenizer.tokenize(variableDescriptionS) ++ datasetTags ++ variableTags

        DatamartEntry(datamartIdentifier, words, datasetDescription, datasetTags, variableDescription, variableTags)
      }.toVector
    }

    new DatamartOntology(datamartEntries)
  }
}
