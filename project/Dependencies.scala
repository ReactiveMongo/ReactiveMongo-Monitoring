import sbt._
import sbt.Keys._

object Dependencies {

  val specsDeps = Def.setting[Seq[ModuleID]] {
    val ver = {
      if (scalaBinaryVersion.value startsWith "3") {
        "5.5.3"
      } else {
        "4.10.6"
      }
    }

    Seq("specs2-core", "specs2-junit").map { n => "org.specs2" %% n % ver }
  }

  val slf4jVer = "1.7.32"

  val slf4jDeps = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVer % Provided,
    "org.slf4j" % "slf4j-simple" % slf4jVer % Test
  )

  val reactiveMongo = Def.setting[ModuleID] {
    "org.reactivemongo" %% "reactivemongo" % version.value % Provided
  }
}
