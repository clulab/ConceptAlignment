import org.clulab.sbt.Resolvers

name := "comparer"
description := "comparer"

resolvers ++= Seq(
  Resolvers.clulabResolver,
  Resolvers.jitpackResolver
)

libraryDependencies ++= {
  Seq(
  )
}
