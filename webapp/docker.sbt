import com.typesafe.sbt.packager.docker.{Cmd, CmdLike, DockerChmodType, DockerPermissionStrategy}

val appDir = "/app"
val binDir = appDir + "/bin/" // The second half is determined by the plug-in.  Don't change.
val app = binDir + "webapp"
val port = 9001

Docker / defaultLinuxInstallLocation := appDir
Docker / dockerBaseImage := "openjdk:8"
Docker / daemonUser := "nobody"
Docker / dockerExposedPorts := List(port)
Docker / dockerEnvVars := Map(
  "_JAVA_OPTIONS" -> "-Xmx16g -Xms12g -Dfile.encoding=UTF-8"
)
Docker / maintainer := "Keith Alcock <docker@keithalcock.com>"
Docker / mappings := (Docker / mappings).value.filter { case (_, string) =>
  // Only allow the app into the /app/bin directory.  Other apps that
  // might be automatically discovered are to be excluded.
  !string.startsWith(binDir) || string == app
}
Docker / version := "0.4.0"

dockerAdditionalPermissions += (DockerChmodType.UserGroupPlusExecute, app)
dockerCmd := Seq(s"-Dhttp.port=$port")
dockerEntrypoint := Seq(app)
dockerPermissionStrategy := DockerPermissionStrategy.None
dockerUpdateLatest := true

// Run "show dockerCommands" and use this to edit as appropriate.
dockerCommands := dockerCommands.value.flatMap { dockerCommand: CmdLike =>
  dockerCommand match {
    case Cmd("USER", "1001:0") =>
      Seq(
        // Make sure that the app can be executed by everyone.
        Cmd("RUN", "chmod", "775", app),
        dockerCommand
      )
    case _ => Seq(dockerCommand)
  }
}
