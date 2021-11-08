
resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies, com.github.jelmerk
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d
)

libraryDependencies ++= {
  val ulihaoyiVer = "0.7.1"

  Seq(
    "com.lihaoyi"             %% "ujson"              % ulihaoyiVer,
    "org.clulab"              %% "eidos"              % "1.3.0" // "1.1.0-SNAPSHOT"
  )
}
