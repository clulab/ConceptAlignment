name := "ConceptAlignment"
organization := "org.clulab"

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d
)

libraryDependencies ++= {
  val     procVer = "8.0.3" // Match transitive dependency in Eidos.
  val   luceneVer = "6.6.6" // Match transitive dependency in Eidos.
  val ulihaoyiVer = "0.7.1"

  Seq(
    "org.clulab"        %% "processors-main"         % procVer,
    "org.clulab"        %% "processors-corenlp"      % procVer,
    "org.clulab"        %% "eidos"                   % "1.0.3", // "1.1.0-SNAPSHOT",
    "ai.lum"            %% "common"                  % "0.0.10",
    "com.lihaoyi"       %% "ujson"                   % ulihaoyiVer,
    "com.lihaoyi"       %% "upickle"                 % ulihaoyiVer,
    "com.lihaoyi"       %% "requests"                % "0.5.1",
    "org.apache.lucene"  % "lucene-core"             % luceneVer,
    "org.apache.lucene"  % "lucene-analyzers-common" % luceneVer,
    "org.apache.lucene"  % "lucene-queryparser"      % luceneVer
  )
}

lazy val core = project in file(".")

/* lazy val webapp = project */
/*   .enablePlugins(PlayScala) */
/*   .aggregate(core) */
/*   .dependsOn(core) */
