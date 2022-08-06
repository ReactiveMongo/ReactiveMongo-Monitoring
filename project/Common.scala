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
    Compile / doc / scalacOptions ++= {
      if (scalaBinaryVersion.value == "3") {
        Seq(
          /*"-diagrams", */"-implicits", "-skip-by-id:highlightextractor")
      } else {
        Seq("-implicits", "-skip-packages", "highlightextractor")
      }
    },
    Compile / doc / scalacOptions ++= Opts.doc.title(name.value),
    resolvers ++= Resolver.sonatypeOssRepos("staging") ++ Resolver.
      sonatypeOssRepos("snapshots") :+ Resolver.typesafeRepo("releases"),
    mimaFailOnNoPrevious := false,
    Test / closeableObject := "Common$",
    Test / testOptions += Tests.Cleanup(cleanup.value),
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
    },
    // Mock silencer for Scala3
    Test / doc / scalacOptions ++= List("-skip-packages", "com.github.ghik"),
    Compile / packageBin / mappings ~= {
      _.filter {
        case (_, path) => !path.startsWith("com/github/ghik")
      }
    },
    Compile / packageSrc / mappings ~= {
      _.filter {
        case (_, path) => path != "silent.scala"
      }
    }
  )

  val cleanup = Def.task[ClassLoader => Unit] {
    val log = streams.value.log

    {cl: ClassLoader =>
      import scala.language.reflectiveCalls

      val objectClass = (Test / closeableObject).value
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
