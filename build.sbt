import Dependencies._

ThisBuild / organization := "org.reactivemongo"

ThisBuild / autoAPIMappings := true

val baseName = "reactivemongo"

name := s"${baseName}-monitoring"

resolvers in ThisBuild ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe repository releases" at "https://repo.typesafe.com/typesafe/releases/")

ThisBuild / mimaFailOnNoPrevious := false

libraryDependencies in ThisBuild ++= (
  reactiveMongo.value +: (slf4jDeps ++ specsDeps))

lazy val jmx = (project in file("jmx")).settings(Seq(
  name := s"${baseName}-jmx",
  description := "ReactiveMongo JMX module",
  scalacOptions += "-P:silencer:globalFilters=.*NodeSetInfo\\ in\\ package\\ nodeset.*;.*name\\ in\\ class\\ MongoConnection.*"
))

lazy val root = (project in file(".")).settings(
  publish := ({}),
  publishTo := None
).aggregate(jmx)
