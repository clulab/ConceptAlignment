import org.clulab.sbt.Resolvers

name := "conceptalignment-webapp"
description := "webapp"

resolvers ++= Seq(
)

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  // rest
  "org.apache.httpcomponents"   % "httpclient"     % "4.5.12",      // up to 4.5.13
  "org.apache.httpcomponents"   % "httpcore"       % "4.4.13",      // up to 4.4.14
  "org.apache.httpcomponents"   % "httpmime"       % "4.5.12"       // up to 4.5.13
)

// This is for development mode.
PlayKeys.devSettings += "play.server.http.idleTimeout" -> "infinite"
