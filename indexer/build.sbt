
name := "indexer"
organization := "org.clulab"

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8")

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies, com.github.jelmerk
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d
)

libraryDependencies ++= {
  Seq(
    "org.clulab"              %% "eidos"              % "1.2.0", // "1.1.0-SNAPSHOT"
    "com.github.WorldModelers" % "Ontologies"         % "master-SNAPSHOT",
    "org.scalatestplus.play"  %% "scalatestplus-play" % "3.1.2" % Test
  )
}
