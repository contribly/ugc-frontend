name := "ugc-frontend"

version := "1.0"

lazy val `ugc-frontend` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(jdbc , cache , ws)

libraryDependencies += "com.typesafe.play" %% "anorm" % "2.4.0"

libraryDependencies += specs2 % Test

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

maintainer in Linux := "Tony McCrae <tony@eelpieconsulting.co.uk>"

packageSummary in Linux := "UGC Frontend"

packageDescription := "UGC Frontend"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2"
