name := "ConceptAlignment"
organization := "org.clulab"

resolvers ++= Seq(
  "jitpack" at "https://jitpack.io", // com.github.WorldModelers/Ontologies
  "Artifactory" at "http://artifactory.cs.arizona.edu:8081/artifactory/sbt-release" // org.clulab/glove-840b-300d
)

val procVer = "8.0.2"

libraryDependencies ++= Seq(
  "org.clulab"    %% "processors-main"          % procVer,
  "org.clulab"    %% "processors-corenlp"       % procVer,
  "org.clulab"    %% "eidos"                    % "1.1.0-SNAPSHOT",
  "ai.lum"        %% "common"                   % "0.0.8",
  "com.lihaoyi"   %% "ujson"                    % "0.7.1",
  "com.lihaoyi"   %% "upickle"                  % "0.7.1",
  "org.scalatest" %% "scalatest"                % "3.0.4" % "test"
)

lazy val core = project in file(".")

/* lazy val webapp = project */
/*   .enablePlugins(PlayScala) */
/*   .aggregate(core) */
/*   .dependsOn(core) */
