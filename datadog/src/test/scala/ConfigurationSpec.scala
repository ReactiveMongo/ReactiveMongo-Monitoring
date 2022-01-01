package reactivemongo.datadog

import com.typesafe.config.ConfigFactory

final class ConfigurationSpec extends org.specs2.mutable.Specification {
  "Configuration".title

  "Configuration" should {
    "not be loaded when missing" in {
      val missingCfg = ConfigFactory.parseProperties(new java.util.Properties())

      Configuration(missingCfg) must beSuccessfulTry(
        Option.empty[Configuration]
      )
    }

    "be loaded" >> {
      "from minimal properties" in {
        val minimalCfg = {
          val props = new java.util.Properties()

          props.put("reactivemongo.datadog.name", "minimal")
          props.put("reactivemongo.datadog.hostname", "localhost")
          props.put("reactivemongo.datadog.port", "8125")

          ConfigFactory.parseProperties(props)
        }

        Configuration(minimalCfg) must beSuccessfulTry(
          Some(
            new Configuration(
              name = "minimal",
              hostname = "localhost",
              port = 8125,
              prefix = "reactivemongo"
            )
          )
        )

      }

      "from complete resource" in {
        Configuration.defaultConfiguration() must beSuccessfulTry(
          Some(
            new Configuration(
              name = "tests",
              hostname = "localhost",
              port = 8126,
              prefix = "mytests",
              constantTags = Seq("bar", "lorem"),
              bufferPoolSize = Some(10),
              entityID = Some("id"),
              queueSize = Some(12),
              senderWorkers = Some(13),
              socketBufferSize = Some(14),
              timeout = Some(2000),
              telemetry =
                Some(new TelemetrySettings(hostname = "localhost", port = 1234))
            )
          )
        )

      }
    }
  }
}
