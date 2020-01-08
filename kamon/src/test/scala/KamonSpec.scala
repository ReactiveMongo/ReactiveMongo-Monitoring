import external.reactivemongo.ConnectionListener

final class KamonSpec extends org.specs2.mutable.Specification {
  "Kamon" title

  sequential

  //import Common.db

  "Connection listener" should {
    lazy val listener = ConnectionListener()

    "be resolved as the default one" in {
      listener must beSome[ConnectionListener].like {
        case _: reactivemongo.kamon.ConnectionListener => ok
      }
    }
  }
}
