name := "webapp"
scalaVersion := "2.12.4"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)

fork in run := false