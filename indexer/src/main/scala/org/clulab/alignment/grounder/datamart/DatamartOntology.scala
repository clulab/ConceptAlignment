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

  def descriptionToStrings(description: String, tokenizer: Tokenizer): Array[String] =
      tokenizer.tokenize(description)

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
          datasetDescription,
          _, // dataset_url
          variableId,
          _, // variable_name
          variableTagsJson,
          variableDescription
        ) = tsvReader.readln(line, length = 10)
        val datamartIdentifier = new DatamartIdentifier(datamartId, datasetId, variableId)
        val datasetTags = tagsToStrings(datasetTagsJson, tokenizer).toArray
        val tokenizedDatasetDescription = descriptionToStrings(datasetDescription, tokenizer)
        val variableTags = tagsToStrings(variableTagsJson, tokenizer).toArray
        val tokenizedVariableDescription = descriptionToStrings(variableDescription, tokenizer)
        // For backward compatability the datasetDescription is not included in the words.
        val words = tokenizedVariableDescription ++ datasetTags ++ variableTags

        DatamartEntry(datamartIdentifier, words, tokenizedDatasetDescription, datasetTags, tokenizedVariableDescription, variableTags)
      }.toVector
    }

    new DatamartOntology(datamartEntries)
  }
}
