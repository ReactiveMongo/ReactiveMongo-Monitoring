package reactivemongo.kamon

import scala.collection.mutable.{ Map => MMap }

import kamon.Kamon
import kamon.metric.MeasurementUnit
import kamon.tag.TagSet
import kamon.trace.Span

import reactivemongo.api.MongoConnectionOptions

import reactivemongo.core.nodeset.NodeSetInfo

/** Listener definition for the connection events. */
final class ConnectionListener
  extends external.reactivemongo.ConnectionListener {

  def poolCreated(
    options: MongoConnectionOptions,
    supervisor: String,
    connection: String): Unit = {
    Kamon.spanBuilder("reactivemongo.pool-created").
      kind(Span.Kind.Internal).
      doNotTrackMetrics().
      tag("name", s"${supervisor}.${connection}").
      start().finish()
  }

  private val poolTags = MMap.empty[(String, String), TagSet]

  private val nodes = MMap.empty[(String, String), Vector[String]]

  @com.github.ghik.silencer.silent("NodeSetInfo")
  def nodeSetUpdated(
    supervisor: String,
    connection: String,
    previous: NodeSetInfo,
    updated: NodeSetInfo): Unit = {

    val prefix = updated.name.foldLeft(
      s"${supervisor}.${connection}") { (p, n) => s"$p.$n" }

    val key = supervisor -> connection

    poolTags.synchronized {
      poolTags.put(key, TagSet.of("pool", prefix))
    }

    traceUpdate(supervisor, connection, updated)

    def pg(property: String, description: String) =
      Kamon.gauge(
        name = s"reactivemongo.${property}",
        description = description).withTag("pool", prefix)

    // Node set gauges
    pg(
      "awaitingRequests",
      "Total number of requests awaiting to be processed by MongoDB nodes").
      update(updated.awaitingRequests.fold(0D)(_.toDouble))

    pg(
      "maxAwaitingRequestsPerChannel",
      "Maximum number of requests that were awaiting to be processed for a single channel (see maxInFlightRequestsPerChannel)").
      update(updated.maxAwaitingRequestsPerChannel.fold(0D)(_.toDouble))

    pg("numberOfNodes", "Number of nodes that are part of the set").
      update(updated.nodes.size.toDouble)

    pg("hasPrimary", "Indicates whether the primary node is known (0 or 1)").
      update(if (updated.primary.isDefined) 1D else 0D)

    pg("hasNearest", "Indicates whether the nearest node is known (0 or 1)").
      update(if (updated.nearest.isDefined) 1D else 0D)

    pg("isMongos", "Indicates whether mongos is used (0 or 1)").
      update(if (updated.mongos.isDefined) 1D else 0D)

    pg("numberOfSecondaries", "Number of secondary nodes in the set").
      update(updated.secondaries.size.toDouble)

    // Node detailed gauges
    nodes.synchronized {
      nodes.put(key, updated.nodes.map(_.name))
    }

    def ng(tag: String, property: String, description: String) =
      Kamon.gauge(
        name = s"reactivemongo.${property}",
        description = description).
        withTag("node", tag)

    updated.nodes.foreach { node =>
      val tag = s"${supervisor}.${connection}.${node.name}"

      ng(tag, "nodeChannels", "Number of network channels to a same MongoDB node, whatever is the status (see connectedChannels)").update(node.connections.toDouble)

      ng(tag, "connectedChannels", "Number of connected channels to a same MongoDB node (see nodeChannels)").update(node.connected.toDouble)

      ng(tag, "authenticatedChannels", "Number of authenticated channels to a same MongoDB node (see connectedChannels)").update(node.authenticated.toDouble)

      Kamon.gauge(
        name = "reactivemongo.pingTime",
        description = "Response delay (in milliseconds) for the last IsMaster request",
        unit = MeasurementUnit.time.milliseconds).withTag("node", tag).update(node.pingInfo.ping.toDouble / 1000D)
    }
  }

  def poolShutdown(supervisor: String, connection: String) = {
    def g(property: String) = Kamon.gauge(s"reactivemongo.${property}")

    val key = supervisor -> connection

    val pts: TagSet = (poolTags.synchronized {
      poolTags.remove(key)
    }).getOrElse(TagSet.Empty)

    // Node set gauges
    g("awaitingRequests").remove(pts)
    g("maxAwaitingRequestsPerChannel").remove(pts)
    g("numberOfNodes").remove(pts)
    g("hasPrimary").remove(pts)
    g("hasNearest").remove(pts)
    g("isMongos").remove(pts)
    g("numberOfSecondaries").remove(pts)

    val ns = (nodes.synchronized {
      nodes.remove(key)
    }).getOrElse(Vector.empty[String])

    ns.foreach { node =>
      val tag = s"${supervisor}.${connection}.${node}"
      val nts = TagSet.of("node", tag)

      g("nodeChannels").remove(nts)
      g("connectedChannels").remove(nts)
      g("authenticatedChannels").remove(nts)
      g("pingTime").remove(nts)
    }

    Kamon.spanBuilder("reactivemongo.pool-stopped").
      kind(Span.Kind.Internal).
      tag("name", s"${supervisor}.${connection}").
      start().finish()

    ()
  }

  @com.github.ghik.silencer.silent("NodeSetInfo")
  private def traceUpdate(
    supervisor: String,
    connection: String,
    updated: NodeSetInfo): Unit = {
    var span = Kamon.spanBuilder("reactivemongo.pool-updated").
      kind(Span.Kind.Internal).
      tag("name", s"${supervisor}.${connection}")

    updated.version.foreach { v =>
      span = span.tag("version", v)
    }

    updated.primary match {
      case Some(node) =>
        span = span.tag("primary", node.name)

      case _ =>
        span = span.tag("primary", "<none>")
    }

    updated.mongos match {
      case Some(node) =>
        span = span.tag("mongos", node.name)

      case _ =>
        span = span.tag("mongos", "<none>")
    }

    updated.nearest match {
      case Some(node) =>
        span = span.tag("nearest", node.name)

      case _ =>
        span = span.tag("nearest", "<unknown>")
    }

    span = span.tag("toString", updated.toString)

    span.start().finish()
  }
}
