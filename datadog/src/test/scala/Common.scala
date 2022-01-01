object Common {
  import scala.concurrent.{ Await, ExecutionContext }
  import scala.concurrent.duration._
  import reactivemongo.api.{
    FailoverStrategy,
    AsyncDriver,
    MongoConnectionOptions
  }

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  val replSetOn = sys.props.get("test.replicaSet").fold(false) {
    case "true" => true
    case _      => false
  }

  val DefaultOptions =
    MongoConnectionOptions.default.copy(nbChannelsPerNode = 2)

  val primaryHost =
    sys.props.getOrElse("test.primaryHost", "localhost:27017")

  val failoverRetries = sys.props
    .get("test.failoverRetries")
    .flatMap(r => scala.util.Try(r.toInt).toOption)
    .getOrElse(7)

  lazy val driver = new AsyncDriver

  lazy val connection =
    Await.result(driver.connect(List(primaryHost), DefaultOptions), timeout)

  val failoverStrategy = FailoverStrategy(retries = failoverRetries)

  val timeout = 10.seconds // increaseTimeoutIfX509(10.seconds)
  val timeoutMillis = timeout.toMillis.toInt

  val dbName = "specs2-reactivemongo-jmx"

  lazy val db = {
    val _db = connection.database(dbName, failoverStrategy)

    Await.result(_db.flatMap { d => d.drop().map(_ => d) }, timeout)
  }

  val onClose = Seq.newBuilder[() => Unit]

  def close(): Unit =
    try {
      Await.result(driver.close(), timeout)

      onClose.result().foreach(_())
    } catch { case _: Throwable => () }
}
