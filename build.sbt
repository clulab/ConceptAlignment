name := "ConceptAlignment"
organization := "org.clulab"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.11", "2.12.4")

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io" // com.github.WorldModelers/Ontologies, com.github.jelmerk
)

libraryDependencies ++= {
  val   luceneVer = "6.6.6" // Match transitive dependency in Eidos.

  Seq(
    "ai.lum"                     %% "common"                  % "0.0.8", // match eidos
    "org.apache.lucene"           % "lucene-core"             % luceneVer,
    "org.apache.lucene"           % "lucene-analyzers-common" % luceneVer,
    "org.apache.lucene"           % "lucene-queryparser"      % luceneVer,

    "com.typesafe.scala-logging" %% "scala-logging"           % "3.7.2",
    "ch.qos.logback"              % "logback-classic"         % "1.0.10",
    "org.slf4j"                   % "slf4j-api"               % "1.7.10",

    "com.github.jelmerk"         %% "hnswlib-scala"           % "0.0.46"
  )
}

lazy val core = project in file(".")

lazy val scraper = project
    .aggregate(core)
    .dependsOn(core)

lazy val indexer = project
    .aggregate(core)
    .dependsOn(core)

lazy val webapp = project
    .enablePlugins(PlayScala)
    .aggregate(core)
    .dependsOn(core)

lazy val evaluator = project
    .aggregate(core)
    .dependsOn(core)
    .aggregate(indexer)
    .dependsOn(indexer)
