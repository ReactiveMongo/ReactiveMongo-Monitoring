ThisBuild / scalaVersion := "2.12.20"

ThisBuild / crossScalaVersions := Seq(
  "2.11.12",
  scalaVersion.value,
  "2.13.15",
  "3.6.3"
)

crossVersion := CrossVersion.binary

ThisBuild / scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings"
)

ThisBuild / scalacOptions ++= {
  val v = scalaBinaryVersion.value

  if (v == "2.13") {
    Seq(
      "-release",
      "8",
      "-Xlint",
      "-g:vars"
    )
  } else if (v startsWith "2.") {
    Seq(
      "-target:jvm-1.8",
      "-Xlint",
      "-g:vars"
    )
  } else {
    Seq("-release", "8")
  }
}

ThisBuild / scalacOptions ++= {
  val sv = scalaBinaryVersion.value

  if (sv == "2.12") {
    Seq(
      "-Xmax-classfile-name",
      "128",
      "-Ywarn-numeric-widen",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-infer-any",
      "-Ywarn-unused",
      "-Ywarn-unused-import",
      "-Ywarn-macros:after"
    )
  } else if (sv == "2.11") {
    Seq(
      "-Xmax-classfile-name",
      "128",
      "-Yopt:_",
      "-Ydead-code",
      "-Yclosure-elim",
      "-Yconst-opt"
    )
  } else if (sv == "2.13") {
    Seq(
      "-explaintypes",
      "-Werror",
      "-Wnumeric-widen",
      "-Wdead-code",
      "-Wvalue-discard",
      "-Wextra-implicit",
      "-Wmacros:after",
      "-Wunused"
    )
  } else {
    Seq(
      "-Wunused:all",
      "-language:implicitConversions",
      "-Wconf:msg=.*is\\ not\\ declared\\ infix.*:s",
      "-Wconf:msg=.*function.*\\ _.*no\\ longer\\ supported.*:s",
      "-Wconf:msg=.*is\\ no\\ longer\\ supported\\ for\\ vararg\\ splices.*:s"
    )
  }
}

Compile / console / scalacOptions ~= {
  _.filterNot(o =>
    o.startsWith("-X") || o.startsWith("-Y") || o.startsWith("-P:silencer")
  )
}

/* TODO: Remove
Test / scalacOptions ~= {
  val excluded = Set("-Xfatal-warnings")

  _.filterNot(excluded.contains)
 }
 */

val filteredScalacOpts: Seq[String] => Seq[String] = {
  _.filterNot { opt => opt.startsWith("-X") || opt.startsWith("-Y") }
}

Compile / console / scalacOptions ~= filteredScalacOpts

Test / console / scalacOptions ~= filteredScalacOpts

// Silencer
ThisBuild / libraryDependencies ++= {
  if (!scalaBinaryVersion.value.startsWith("3")) {
    val silencerVersion = "1.7.19"

    Seq(
      compilerPlugin(
        ("com.github.ghik" %% "silencer-plugin" % silencerVersion)
          .cross(CrossVersion.full)
      ),
      ("com.github.ghik" %% "silencer-lib" % silencerVersion % Provided)
        .cross(CrossVersion.full)
    )
  } else Seq.empty
}
