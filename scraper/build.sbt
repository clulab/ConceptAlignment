
name := "scraper"
organization := "org.clulab"

scalaVersion := "2.12.4"
crossScalaVersions := Seq("2.11.11", "2.12.4")

libraryDependencies ++= {
  val ulihaoyiVer = "0.7.1"

  Seq(
    "com.lihaoyi"        %% "ujson"                   % ulihaoyiVer,
    "com.lihaoyi"        %% "upickle"                 % ulihaoyiVer,
    "com.lihaoyi"        %% "requests"                % "0.5.1"
  )
}
