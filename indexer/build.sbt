import org.clulab.sbt.Resolvers

name := "conceptalignment-indexer"
description := "Code to index data used for concept alignment"

resolvers ++= Seq(
  Resolvers.clulabResolver, // org.clulab/glove-840b-300d
  Resolvers.jitpackResolver // com.github.WorldModelers/Ontologies, com.github.jelmerk
)

libraryDependencies ++= {
  Seq(
    "org.clulab"              %% "eidos"              % "1.5.0", // "1.1.0-SNAPSHOT"
    // Only change this if you are prepared to reindex.
    "com.github.WorldModelers" % "Ontologies"         % "3.0",
    "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.2" % Test
  )
}
