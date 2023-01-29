package reactivemongo.datadog

final class TelemetrySettings private[datadog] (
    val hostname: String,
    val port: Int) {

  private lazy val tupled = hostname -> port

  override def toString: String = s"Telemetry { host: '${hostname}:${port}' }"

  override def equals(that: Any): Boolean = that match {
    case other: TelemetrySettings =>
      other.tupled == this.tupled

    case _ => false
  }

  override def hashCode: Int = tupled.hashCode
}

final class Configuration private[datadog] (
    val name: String,
    val hostname: String,
    val port: Int,
    val prefix: String,
    val constantTags: Seq[String] = Seq.empty,
    val bufferPoolSize: Option[Int] = None,
    val entityID: Option[String] = None,
    val queueSize: Option[Int] = None,
    val senderWorkers: Option[Int] = None,
    val socketBufferSize: Option[Int] = None,
    val timeout: Option[Int] = None,
    val telemetry: Option[TelemetrySettings] = None) {

  override lazy val toString: String = {
    val buffer = Seq.newBuilder[String]

    if (constantTags.nonEmpty) {
      buffer += s"""constant-tags: ${constantTags.mkString("[", ", ", "]")}"""
    }

    bufferPoolSize.foreach { sz => buffer += s"buffer-pool-size: $sz" }

    entityID.foreach { id => buffer += s"entity-id: '$id'" }

    queueSize.foreach { sz => buffer += s"queue-size: $sz" }

    senderWorkers.foreach { w => buffer += s"sender-workers: $w" }

    socketBufferSize.foreach { sz => buffer += s"socket-buffer-size: $sz" }

    timeout.foreach { t => buffer += s"timeout: $t milliseconds" }

    telemetry.foreach { t => buffer += s"telemetry: $t" }

    val opts = buffer.result().mkString(", ")
    val base =
      s"Configuration { name: '$name', host: '$hostname:$port', prefix = '$prefix'"

    if (opts.isEmpty) {
      s"${base} }"
    } else {
      s"${base}, $opts }"
    }
  }

  override def equals(that: Any): Boolean = that match {
    case other: Configuration =>
      this.tupled == other.tupled

    case _ =>
      false
  }

  override def hashCode: Int = tupled.hashCode

  private lazy val tupled = Tuple12(
    name,
    hostname,
    port,
    prefix,
    constantTags,
    bufferPoolSize,
    entityID,
    queueSize,
    senderWorkers,
    socketBufferSize,
    timeout,
    telemetry
  )
}

object Configuration {
  import scala.util.Try
  import scala.util.control.NonFatal
  import com.typesafe.config.{ Config, ConfigException, ConfigFactory }

  private val DEFAULT_PREFIX = "reactivemongo"

  private lazy val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def defaultConfiguration(): Try[Option[Configuration]] =
    Configuration(ConfigFactory.defaultApplication())

  def apply(load: => Config): Try[Option[Configuration]] = {
    lazy val cfg = load.getConfig("reactivemongo.datadog").resolve()

    def opt[T](f: => T): Option[T] =
      try {
        Option(f)
      } catch {
        case _: ConfigException.Missing =>
          Option.empty[T]
      }

    def str(n: String) = opt(cfg.getString(n))
    def int(n: String) = opt(cfg.getInt(n))

    def telemetry = opt(cfg.getBoolean("telemetry.enabled")).flatMap {
      case false =>
        Option.empty[TelemetrySettings]

      case _ =>
        (for {
          hostname <- str("telemetry.hostname")
          port <- int("telemetry.port")
        } yield new TelemetrySettings(hostname, port)).orElse {
          logger.warn("Telemetry enabled but invalid settings; Check 'reactivemongo.datadog.telemetry.{hostname,port}' values")

          None
        }
    }

    def name: String = str("name").getOrElse {
      try {
        java.net.InetAddress.getLocalHost().getHostName()
      } catch {
        case NonFatal(_) =>
          s"reactivemongo-${System identityHashCode load}"
      }
    }

    Try(str("hostname")).map(_.map { hostname =>
      val tags = opt {
        val buf = Seq.newBuilder[String]

        val it = cfg.getStringList("constantTags").iterator()

        while (it.hasNext) {
          buf += it.next()
        }

        buf.result()
      }

      new Configuration(
        name = name,
        hostname = hostname,
        port = cfg.getInt("port"),
        prefix = str("prefix").getOrElse(DEFAULT_PREFIX),
        constantTags = tags.getOrElse(Seq.empty),
        bufferPoolSize = int("bufferPoolSize"),
        entityID = str("entityID"),
        queueSize = int("queueSize"),
        senderWorkers = int("senderWorkers"),
        socketBufferSize = int("socketBufferSize"),
        timeout = int("timeout"),
        telemetry = telemetry
      )
    })
  }
}
