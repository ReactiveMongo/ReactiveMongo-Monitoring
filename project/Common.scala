import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.xml.{ Elem => XmlElem, Node => XmlNode }

import com.typesafe.tools.mima.plugin.MimaKeys.mimaFailOnNoPrevious

object Common extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  val closeableObject = SettingKey[String]("class name of a closeable object")

  override def projectSettings = Seq(
    organization := "org.reactivemongo",
    autoAPIMappings := true,
    testFrameworks ~= { _.filterNot(_ == TestFrameworks.ScalaTest) },
    scalacOptions ++= {
      val v = scalaBinaryVersion.value

      if (v == "2.12") {
        Seq(
          "-Ywarn-numeric-widen",
          "-Ywarn-dead-code",
          "-Ywarn-value-discard",
          "-Ywarn-infer-any",
          "-Ywarn-unused",
          "-Ywarn-unused-import",
          "-Ywarn-macros:after"
        )
      } else if (v == "2.11") {
        Seq("-Yopt:_", "-Ydead-code", "-Yclosure-elim", "-Yconst-opt")
      } else if (v != "2.10") {
        Seq("-Wmacros:after")
      } else {
        Seq.empty
      }
    },
    scalacOptions in (Compile, doc) := (scalacOptions in Test).value ++ Seq(
      "-unchecked", "-deprecation",
      /*"-diagrams", */"-implicits", "-skip-packages", "highlightextractor") ++
      Opts.doc.title(name.value),
    /*
    unmanagedSourceDirectories in Compile += {
      val base = (sourceDirectory in Compile).value

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => base / "scala-2.13+"
        case _                       => base / "scala-2.13-"
      }
    },*/
    resolvers ++= Seq(
      Resolver.sonatypeRepo("staging"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.typesafeRepo("releases")),
    mimaFailOnNoPrevious := false,
    closeableObject in Test := "Common$",
    testOptions in Test += Tests.Cleanup(cleanup.value),
    pomPostProcess := {
      val next: XmlElem => Option[XmlElem] = providedInternalDeps

      val f: XmlElem => Option[XmlElem] = { dep =>
        if ((dep \ "artifactId").text startsWith "silencer-lib") {
          Option.empty[XmlElem]
        } else {
          next(dep)
        }
      }

      XmlUtil.transformPomDependencies(f)
    }

  )

  val cleanup = Def.task[ClassLoader => Unit] {
    val log = streams.value.log

    {cl: ClassLoader =>
      import scala.language.reflectiveCalls

      val objectClass = (closeableObject in Test).value
      val c = cl.loadClass(objectClass)
      type M = { def close(): Unit }
      val m: M = c.getField("MODULE$").get(null).asInstanceOf[M]

      log.info(s"Closing $m ...")

      m.close()
    }
  }

  // ---

  private lazy val providedInternalDeps: XmlElem => Option[XmlElem] = {
    import scala.xml.NodeSeq
    import scala.xml.transform.{ RewriteRule, RuleTransformer }

    val asProvided = new RuleTransformer(new RewriteRule {
      override def transform(node: XmlNode): NodeSeq = node match {
        case e: XmlElem if e.label == "scope" =>
          NodeSeq.Empty

        case _ => node
      }
    })

    { dep: XmlElem =>
      if ((dep \ "groupId").text == "org.reactivemongo") {
        asProvided.transform(dep).headOption.collectFirst {
          case e: XmlElem => e.copy(
            child = e.child :+ <scope>provided</scope>)
        }
      } else Some(dep)
    }
  }
}
