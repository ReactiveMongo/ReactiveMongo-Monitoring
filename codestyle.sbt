ThisBuild / scalafmtOnCompile := true

inThisBuild(
  List(
    // scalaVersion := "2.13.3",
    semanticdbEnabled := scalaBinaryVersion.value != "2.11",
    semanticdbVersion := scalafixSemanticdb.revision,
  )
)
