organization := "com.github.krasserm"

name := "akka-persistence-testkit"

version := "0.4-SNAPSHOT"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.10.4", "2.11.8")

fork := true

parallelExecution in Test := false

libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.4.7" % "compile"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.7" % "compile"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.4" % "compile"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"

libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.7" % "test"

libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8" % "test"
