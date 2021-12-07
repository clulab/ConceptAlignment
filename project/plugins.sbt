// Github testing doesn't like this, so the version is hard-coded.
// import org.clulab.sbt.BuildUtils

// val playVersion = BuildUtils.sbtPluginVersion

addSbtPlugin("com.github.gseitz" % "sbt-release"          % "1.0.13")    // up to 1.0.13
addSbtPlugin("com.jsuereth"      % "sbt-pgp"              % "1.1.2-1")   // up to 1.1.2-1 *
addSbtPlugin("com.typesafe.play" % "sbt-plugin"           % "2.7.4")
addSbtPlugin("com.typesafe.sbt"  % "sbt-native-packager"  % "1.7.6")     // up to 1.8.1
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.9.2")     // up to 0.9.2 !
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"         % "2.3")       // up to 3.9.6 *
