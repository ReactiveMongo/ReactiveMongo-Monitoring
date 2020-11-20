import external.reactivemongo.ConnectionListener

final class DatadogSpec extends org.specs2.mutable.Specification {
  "Datadog" title

  sequential

  //import Common.db

  "Connection listener" should {
    lazy val listener = ConnectionListener()

    "be resolved as the default one" in {
      listener must beSome[ConnectionListener].like {
        case _: reactivemongo.datadog.ConnectionListener => ok
      }
    }
  }
}
