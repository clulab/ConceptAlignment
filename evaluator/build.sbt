import org.clulab.sbt.Resolvers

name := "conceptalignment-evaluator"
description := "evaluator"

resolvers ++= Seq(
)

libraryDependencies ++= {
  val procVer = "8.4.3" // Match transitive dependency in Eidos.

  Seq(
    "org.clulab"         %% "processors-main"         % procVer,
    "org.clulab"         %% "processors-corenlp"      % procVer,
    "org.clulab"         %% "eidos"                   % "1.5.0",
    "org.scalatest"      %% "scalatest"               % "3.0.4" % "test"
  )
}
