package org.clulab.alignment.webapp.controllers.utils

import java.io.File

import collection.JavaConverters._

class ExternalProcess(commands: Seq[String]) {
  protected val processBuilder: ProcessBuilder = new ProcessBuilder(commands.asJava)

  def this() = this(Seq.empty[String])

  def this(command: String) = this(Seq(command))

  def execute(): Process = {
    val process = processBuilder.start()

    process
  }

  def command(): Seq[String] = processBuilder.command().asScala

  def directory(name: String): Unit = processBuilder.directory(new File(name))

  def environment(name: String, value: String): Unit = processBuilder.environment.put(name, value)
}
