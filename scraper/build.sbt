import org.clulab.sbt.Resolvers

name := "conceptalignment-scraper"
description := "Code to scrape data used for concept alignment"

libraryDependencies ++= {
  val ulihaoyiVer = "0.7.1"

  Seq(
    "com.lihaoyi"        %% "ujson"                   % ulihaoyiVer,
    "com.lihaoyi"        %% "upickle"                 % ulihaoyiVer,
    "com.lihaoyi"        %% "requests"                % "0.5.1"
  )
}
