package org.clulab.alignment.controllers.utils

import java.io.File
import java.io.FilenameFilter
import java.nio.file.Files
import java.nio.file.Paths

object FileUtils {

  def findIndex(searchDir: String, prefix: String): Option[(File, Int)] = {
    // Do a foldLeft, starting with None
    val walk = Files.walk(Paths.get(searchDir), 1)
    walk.forEach { path =>
      if (Files.isDirectory(path)) {
        val file = path.toFile
        val name = file.getName
        if (name.startsWith(prefix)) {
          val suffix = name.substring(prefix.length)
          val indexOpt = try {
            Some(suffix.toInt)
          }
          catch {
            case throwable: Throwable => None
          }
          if (indexOpt.isDefined && indexOpt.get.toString == suffix) {
            val index = indexOpt.get

            println(index)
          }
        }
      }
    }
    null
  }
}
