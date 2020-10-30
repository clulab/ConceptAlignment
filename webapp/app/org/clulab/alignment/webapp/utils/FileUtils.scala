package org.clulab.alignment.webapp.utils

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

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

  // See https://mkyong.com/java/how-to-delete-directory-in-java/
  // and https://alvinalexander.com/scala/search-directory-tree-SimpleFileVisitor-walkFileTree-recursive/.
  class Deleter extends SimpleFileVisitor[Path] {

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
      Files.delete(dir)
      FileVisitResult.CONTINUE
    }

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.delete(file)
      FileVisitResult.CONTINUE
    }
  }

  def rmdir(dirname: String): Unit = {
    val path = Paths.get(dirname)
    Files.walkFileTree(path, new Deleter())
  }
}
