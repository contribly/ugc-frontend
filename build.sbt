name := "ugc-frontend"

version := "1.0"

lazy val `ugc-frontend` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(ws)

libraryDependencies ++= Seq("com.restfb" % "restfb" % "1.19.0")

libraryDependencies ++= Seq("org.twitter4j" % "twitter4j-core" % "4.0.4")

libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.12"

libraryDependencies += specs2 % Test

maintainer in Linux := "Tony McCrae <tony@eelpieconsulting.co.uk>"

packageSummary in Linux := "UGC Frontend"

packageDescription := "UGC Frontend"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.2"

import com.typesafe.sbt.packager.archetypes.ServerLoader

serverLoading in Debian:= ServerLoader.Systemd
