import org.clulab.sbt.Resolvers

name := "evaluator"
description := "comparer"

resolvers ++= Seq(
)

libraryDependencies ++= {
  val procVer = "8.4.3" // Match transitive dependency in Eidos.

  Seq(
    "org.clulab"         %% "processors-main"         % procVer,
    "org.clulab"         %% "processors-corenlp"      % procVer,
    "org.clulab"         %% "eidos"                   % "1.3.0a",
    "org.scalatest"      %% "scalatest"               % "3.0.4" % "test"
  )
}
