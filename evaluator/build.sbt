
name := "evaluator"
organization := "org.clulab"

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8")

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies, com.github.jelmerk
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d, eidos
)

libraryDependencies ++= {
  val procVer = "8.4.3" // Match transitive dependency in Eidos.

  Seq(
    "org.clulab"         %% "processors-main"         % procVer,
    "org.clulab"         %% "processors-corenlp"      % procVer,
    "org.clulab"         %% "eidos"                   % "1.3.0",
    "org.scalatest"      %% "scalatest"               % "3.0.4" % "test"
  )
}
