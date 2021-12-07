import org.clulab.sbt.Resolvers

name := "builder"
description := "builder"

resolvers ++= Seq(
  Resolvers.clulabResolver,
  Resolvers.jitpackResolver
)

libraryDependencies ++= {
  Seq(
  )
}
