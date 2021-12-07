import org.clulab.sbt.BuildUtils
import org.clulab.sbt.Resolvers

name := "conceptalignment-core"
description := BuildUtils.singleLine("""
  |This project is  used to align concepts from a top-down program ontology to bottom-up
  |concepts/indicators/variables/model components.
""")

// Last checked 2021-08-23
val scala11 = "2.11.12" // up to 2.11.12
val scala12 = "2.12.14" // up to 2.12.14
val scala13 = "2.13.6"  // up to 2.13.6

// Processors is not available for scala13, so it is skipped here.
ThisBuild / crossScalaVersions := Seq(scala12, scala11) // , scala13)
ThisBuild / scalaVersion := crossScalaVersions.value.head

resolvers ++= Seq(
  Resolvers.clulabResolver, // org.clulab/glove-840b-300d
  Resolvers.jitpackResolver // com.github.WorldModelers/Ontologies, com.github.jelmerk
)

libraryDependencies ++= {
  val luceneVer = "6.6.6" // Match transitive dependency in Eidos.

  Seq(
    "ai.lum"                     %% "common"                  % "0.0.8", // match eidos
    "org.apache.lucene"           % "lucene-core"             % luceneVer,
    "org.apache.lucene"           % "lucene-analyzers-common" % luceneVer,
    "org.apache.lucene"           % "lucene-queryparser"      % luceneVer,

    "com.typesafe.scala-logging" %% "scala-logging"           % "3.7.2",
    "com.typesafe.play"          %% "play-json"               % "2.8.0", // match the plug-in

    "ch.qos.logback"              % "logback-classic"         % "1.0.10",
    "org.slf4j"                   % "slf4j-api"               % "1.7.10",

    "com.github.jelmerk"         %% "hnswlib-scala"           % "0.0.46",

    "org.scalatest"              %% "scalatest"               % "3.0.4" % "test"
  )
}

lazy val builder = project
    .dependsOn(scraper, indexer)

lazy val comparer = project
    .dependsOn(webapp)

lazy val core = (project in file("."))

lazy val scraper = project
    .dependsOn(core)

lazy val indexer = project
    .dependsOn(core)

lazy val webapp = project
    .enablePlugins(PlayScala)
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .aggregate(core)
    .dependsOn(core % "compile -> compile; test -> test", scraper, indexer)

//lazy val jclapp = project
//    .enablePlugins(PlayScala)
//    .dependsOn(core, scraper, indexer)

lazy val evaluator = project
    .dependsOn(core, indexer)

lazy val experiment = project
    .dependsOn(core % "compile -> compile; test -> test", scraper, indexer)

addCommandAlias("dockerize", ";compile;test;webapp/docker:publishLocal")
addCommandAlias("publishAllLocal", ";core/publishLocal;indexer/publishLocal;scraper/publishLocal;webapp/publishLocal")
