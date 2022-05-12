import NativePackagerHelper._
import com.typesafe.sbt.packager.MappingsHelper.directory
import com.typesafe.sbt.packager.docker.{Cmd, CmdLike, DockerChmodType, DockerPermissionStrategy}

val topDir = "/conceptalignment"
val appDir = topDir + "/app"
val binDir = appDir + "/bin/" // The second half is determined by the plug-in.  Don't change.
val app = binDir + "conceptalignment-webapp"
val port = 9001
val tag = "1.5.6"

Docker / defaultLinuxInstallLocation := appDir
Docker / dockerBaseImage := "openjdk:8"
Docker / daemonUser := "nobody"
Docker / dockerExposedPorts := List(port)
Docker / maintainer := "Keith Alcock <docker@keithalcock.com>"
Docker / mappings := (Docker / mappings).value.filter { case (_, string) =>
  // Only allow the app into the /app/bin directory.  Other apps that
  // might be automatically discovered are to be excluded.
  !string.startsWith(binDir) || string == app
}
Docker / packageName := "conceptalignment"
Docker / version := tag

dockerAdditionalPermissions += (DockerChmodType.UserGroupPlusExecute, app)
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerCmd := Seq(s"-Dhttp.port=$port")
dockerEntrypoint := Seq(app)
dockerEnvVars := Map(
  "_JAVA_OPTIONS" -> "-Xmx16g -Xms12g -Dfile.encoding=UTF-8"
)
dockerPermissionStrategy := DockerPermissionStrategy.MultiStage
dockerUpdateLatest := true

// Run "show dockerCommands" and use this to edit as appropriate.
dockerCommands := dockerCommands.value.flatMap { dockerCommand: CmdLike =>
  // Run "show dockerCommands" and use this to edit as appropriate.
  val oldDir = appDir
  val newDir = topDir

  dockerCommand match {
    // This is necessary to reach outside app to its parent directory.
    case cmd @ Cmd("COPY", oldArgs @ _*) =>
      if (oldArgs.length == 1) {
        val args = oldArgs.head.split(' ')
        if (args.length == 4 && args(2) == oldDir && args(3) == oldDir) {
          Seq(Cmd("COPY", args(0), args(1), newDir, newDir))
        }
        else Seq(cmd)
      }
      else Seq(cmd)
    // Make sure that the topDir can be written for reindexing.
    // case Cmd("USER", oldArgs @ _*) if (oldArgs.length == 1 && oldArgs.head == "1001:0") =>
    case Cmd("USER", "1001:0") =>
      Seq(
        Cmd("RUN", "chmod", "775", topDir),
        dockerCommand
      )
    case _ =>
      Seq(dockerCommand)
  }
}

Universal / mappings ++= {

  def moveFile(filename: String): (File, String) = {
    file(filename) -> filename
  }

  Seq(
    moveFile("../hnswlib-glove.idx"),
    moveFile("../hnswlib-wm_flattened.idx"),
    moveFile("../hnswlib-concept.idx"),
    moveFile("../hnswlib-process.idx"),
    moveFile("../hnswlib-property.idx")
  )
}

def moveDir(dirname: String): Seq[(File, String)] = {
  val dir = file(dirname)
  val result = dir
      .**(AllPassFilter)
      .pair(relativeTo(dir.getParentFile))
      .map { case (file, _) => (file, file.getPath) }

//  result.foreach { case (file, string) =>
//    println(s"$file -> $string")
//  }
  result
}

Universal / mappings ++= moveDir("../index_0")
Universal / mappings ++= moveDir("../index_1")
// Don't copy the credentials in case image is published.
// Universal / mappings ++= moveDir("../credentials")

Global / excludeLintKeys += Docker / dockerBaseImage
