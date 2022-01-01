import Dependencies._

organization := "org.reactivemongo"

autoAPIMappings := true

val baseName = "reactivemongo"

name := s"${baseName}-monitoring"

ThisBuild / resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe repository releases" at "https://repo.typesafe.com/typesafe/releases/")

mimaFailOnNoPrevious := false

ThisBuild / libraryDependencies ++= (
  reactiveMongo.value +: (slf4jDeps ++ specsDeps))

lazy val jmx = (project in file("jmx")).settings(
  name := s"${baseName}-jmx",
  description := "ReactiveMongo JMX",
  scalacOptions ++= {
    if (scalaBinaryVersion.value startsWith "2.") {
      Seq("-P:silencer:globalFilters=.*NodeSetInfo\\ in\\ package\\ nodeset.*;.*name\\ in\\ class\\ MongoConnection.*")
    } else {
      Seq.empty
    }
  }
)

lazy val kamon = (project in file("kamon")).settings(
  name := s"${baseName}-kamon",
  description := "ReactiveMongo Kamon",
  libraryDependencies += (
    "io.kamon" %% "kamon-core" % "2.0.4").cross(
    CrossVersion.for3Use2_13) % Provided
)

lazy val datadog = (project in file("datadog")).settings(
  name := s"${baseName}-datadog",
  description := "ReactiveMongo Datadog",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.4.1",
    "com.datadoghq" % "java-dogstatsd-client" % "2.10.3")
)

lazy val root = (project in file(".")).settings(
  publish := ({}),
  publishTo := None,
  Test / testOptions := Seq.empty
).aggregate(jmx, kamon, datadog)
