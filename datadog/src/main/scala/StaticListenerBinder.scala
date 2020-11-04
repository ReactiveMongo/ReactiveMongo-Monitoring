package external.reactivemongo

import scala.util.{ Failure, Success }

import com.timgroup.statsd.{
  NoOpStatsDClient,
  NonBlockingStatsDClient,
  NonBlockingStatsDClientBuilder,
  StatsDClient
}

import reactivemongo.datadog.Configuration

final class StaticListenerBinder {
  private lazy val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  /** !! Unsafe: Throw exception if configuration cannot be found */
  @SuppressWarnings(Array("TryGet"))
  def connectionListener(): ConnectionListener = {
    val (name, client): (String, StatsDClient) =
      Configuration.defaultConfiguration() match {
        case Success(Some(config)) => {
          logger.info(s"Starting connection listener: ${config}")

          val builder = new NonBlockingStatsDClientBuilder()
            .prefix(config.prefix)
            .hostname(config.hostname)
            .port(config.port)

          config.name -> buildWithOptions(builder, config)
        }

        case Failure(cause) => {
          logger.warn("Fails to load configuration", cause)

          Tuple2(
            s"reactivemongo-${System identityHashCode cause}",
            new NoOpStatsDClient())
        }

        case res => {
          logger.info("Connection listener is ignored: no configuration")

          Tuple2(
            s"reactivemongo-${System identityHashCode res}",
            new NoOpStatsDClient())
        }
      }

    new reactivemongo.datadog.ConnectionListener(name, client)
  }

  // ---

  private type BuilderChain = Seq[NonBlockingStatsDClientBuilder => Option[NonBlockingStatsDClientBuilder]]

  private def buildWithOptions(
    builder: NonBlockingStatsDClientBuilder,
    config: Configuration): NonBlockingStatsDClient =
    optionBuilders(config).foldLeft(builder) { (b1, makeOpt) =>
      makeOpt(b1) match {
        case Some(b2) => b2
        case _ => b1
      }
    }.build()

  private def optionBuilders(config: Configuration): BuilderChain =
    Seq(
      b => Some(b.constantTags(config.constantTags: _*)),
      b => config.bufferPoolSize.map(b.bufferPoolSize(_)),
      b => config.entityID.map(b.entityID(_)),
      b => config.queueSize.map(b.queueSize(_)),
      b => config.senderWorkers.map(b.senderWorkers(_)),
      b => config.socketBufferSize.map(b.socketBufferSize(_)),
      b => config.timeout.map(b.timeout(_)),
      { b =>
        config.telemetry.map { t =>
          b.telemetryHostname(t.hostname).
            telemetryPort(t.port)
        }
      })
}
