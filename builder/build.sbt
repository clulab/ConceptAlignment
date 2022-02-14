import org.clulab.sbt.Resolvers

name := "conceptalignment-builder"
description := "builder"

resolvers ++= Seq(
  Resolvers.clulabResolver,
  Resolvers.jitpackResolver
)

libraryDependencies ++= {
  Seq(
  )
}
