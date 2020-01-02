import sbt._
import sbt.Keys._

object Dependencies {
  val specsVer = "4.8.1"

  val specsDeps = Seq(
    "org.specs2" %% "specs2-core" % specsVer,
    "org.specs2" %% "specs2-junit" % specsVer)

  val slf4jVer = "1.7.30"

  val slf4jDeps = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVer % Provided,
    "org.slf4j" % "slf4j-simple" % slf4jVer % Test)

  val reactiveMongo = Def.setting[ModuleID] {
    "org.reactivemongo" %% "reactivemongo" % version.value % Provided
  }
}
