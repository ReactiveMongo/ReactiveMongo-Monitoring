ThisBuild / scalafmtOnCompile := true

inThisBuild(
  List(
    // scalaVersion := "2.13.3",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies ++= Seq(
      "com.github.liancheng" %% "organize-imports" % "0.5.0"
    )
  )
)
