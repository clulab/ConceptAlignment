package org.clulab.alignment.searcher.lucene

import org.clulab.alignment.Test
import org.clulab.alignment.data.datamart.DatamartIdentifier
import org.clulab.alignment.searcher.lucene.document.DatamartDocument

class TestLuceneSearcher extends Test {
  val luceneSearcher = new LuceneSearcher("../lucene-datamart-withgeotime", "variableDescription")

  def getDatamartDocumentsFromIds(datamartIdentifiers: Seq[DatamartIdentifier]): Seq[DatamartDocument] = {
    luceneSearcher.withReader { reader =>
      datamartIdentifiers.map { datamartIdentifier =>
        val document = luceneSearcher.find(reader, datamartIdentifier)

        new DatamartDocument(document)
      }
    }
  }

  behavior of "LuceneSearcher"

  ignore should "find a document by ID" in {
    val datamartIdentifiers = Seq(
      DatamartIdentifier("DOJO_Model", "1ebd2d40-6ba1-48d9-b41b-ca008c75c6c5", "fertilizer_amount_addition"),
      DatamartIdentifier("DOJO_Model", "2ddd2cbe-364b-4520-a28e-a5691227db39", "basin"),
      DatamartIdentifier("DOJO_Indicator", "ETH-CENSUS", "tot_pop"),
      DatamartIdentifier("DOJO_Indicator", "61de8d62-ca47-4f89-956a-f9a3c9a4b495", "seas_S_pss_IO")
    )
    val datamartDocuments = getDatamartDocumentsFromIds(datamartIdentifiers)

    datamartDocuments should have size (datamartDocuments.size)
  }

  ignore should "get all documents" in {
    val iterator = luceneSearcher.datamartSearch("*:*", maxHits = 1000)

    iterator.foreach { case (score, datamartDocument) =>
      println(datamartDocument.datamartIdentifier.toString)
    }
  }

  ignore should "search for countries" in {
    val datamartIdentifiers = luceneSearcher.search(Array("kenya", "Ethiopia"), None, None)

    datamartIdentifiers should not be empty
  }

  it should "search for times" in {
    {
      // This is the maximum value in the data
      val datamartIdentifiers = luceneSearcher.search(Array.empty, Some(4102444800000L), None)

      datamartIdentifiers should not be empty
    }
//    {
//      val datamartIdentifiers = luceneSearcher.search(Array.empty, None, Some(100000))
//
//      datamartIdentifiers should not be empty
//    }
//    {
//      val datamartIdentifiers = luceneSearcher.search(Array.empty, Some(0), Some(100000))
//
//      datamartIdentifiers should not be empty
//    }
  }

  /**
   * 1640995200000	4102444800000
-3155673600000	873072000

   */
}
