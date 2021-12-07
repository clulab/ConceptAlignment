import org.clulab.sbt.Resolvers

name := "conceptalignment-webapp"
description := "webapp"

resolvers ++= Seq(
)

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)
