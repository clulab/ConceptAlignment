
name := "builder"
organization := "org.clulab"

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8")

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies, com.github.jelmerk
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d, eidos
)

libraryDependencies ++= {
  Seq(
  )
}
