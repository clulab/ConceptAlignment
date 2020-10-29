package org.clulab.alignment.webapp

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import scala.collection.JavaConverters._

object FileUtils {

  def findFileAndIndex(baseDir: String, prefix: String): Option[(File, Int)] = {
    val start: Option[(File, Int)] = None
    val walk = Files.walk(Paths.get(baseDir), 1)
    val fileAndIndexOpt = walk.iterator.asScala.foldLeft(start) { case (current, path) =>
      if (Files.isDirectory(path)) {
        val file = path.toFile
        val name = file.getName
        if (name.startsWith(prefix)) {
          val suffix = name.substring(prefix.length)
          val indexOpt = try {
            Some(suffix.toInt)
          }
          catch {
            case exception: NumberFormatException => None
          }
          if (indexOpt.isDefined && indexOpt.get.toString == suffix) {
            val index = indexOpt.get
            if (current.isEmpty || current.get._2 < index)
              Some(file, index)
            else current
          }
          else current
        }
        else current
      }
      else current
    }
    fileAndIndexOpt
  }

  def configure(): Unit = {
    println("Configuring...")

    def configure(filename: String): Unit = {
      val canonicalPath = new java.io.File(filename).getCanonicalPath
      println(s"$filename maps to $canonicalPath")
    }

    configure(".")
  }
}
