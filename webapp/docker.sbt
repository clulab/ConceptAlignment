import NativePackagerHelper._
import com.typesafe.sbt.packager.MappingsHelper.directory
import com.typesafe.sbt.packager.docker.{Cmd, CmdLike, DockerChmodType, DockerPermissionStrategy}

val appDir = "/conceptalignment/app"
val binDir = appDir + "/bin/" // The second half is determined by the plug-in.  Don't change.
val app = binDir + "webapp"
val port = 9001

Docker / defaultLinuxInstallLocation := appDir
Docker / dockerBaseImage := "openjdk:8"
//Docker / daemonUser := "nobody"
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
Docker / packageName := "conceptalignment"
Docker / version := "1.4.0"

dockerAdditionalPermissions += (DockerChmodType.UserGroupPlusExecute, app)
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerCmd := Seq(s"-Dhttp.port=$port")
dockerEntrypoint := Seq(app)
dockerPermissionStrategy := DockerPermissionStrategy.MultiStage
dockerUpdateLatest := true

// Run "show dockerCommands" and use this to edit as appropriate.
//dockerCommands := dockerCommands.value.flatMap { dockerCommand: CmdLike =>
//  dockerCommand match {
//    case Cmd("USER", "1001:0") =>
//      Seq(
//         Make sure that the app can be executed by everyone.
//        Cmd("RUN", "chmod", "775", app),
//        dockerCommand
//      )
//    case _ => Seq(dockerCommand)
//  }
//}

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
Universal / mappings ++= moveDir("../credentials")
