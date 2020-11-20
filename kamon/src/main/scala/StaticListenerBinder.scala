package external.reactivemongo

final class StaticListenerBinder {
  private lazy val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def connectionListener(): ConnectionListener = {
    logger.info("Starting connection listener ...")

    new reactivemongo.kamon.ConnectionListener()
  }
}
