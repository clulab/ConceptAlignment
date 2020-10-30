package org.clulab.alignment.webapp.searcher

import org.clulab.alignment.searcher.knn.KnnLocations
import org.clulab.alignment.webapp.WebappLocations

class SearcherLocations(index: Int = 0, baseDir: String = WebappLocations.baseDir, baseFile: String = WebappLocations.baseFile)
    extends KnnLocations(index, baseDir, baseFile)
