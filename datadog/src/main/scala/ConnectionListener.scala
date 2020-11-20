package reactivemongo.datadog

import scala.collection.immutable.Set
import scala.collection.mutable.{ Map => MMap }

import com.timgroup.statsd.{ Event, StatsDClient }

import reactivemongo.api.MongoConnectionOptions

import reactivemongo.core.nodeset.NodeSetInfo

/** Listener definition for the connection events. */
final class ConnectionListener(hostname: String, client: StatsDClient)
  extends external.reactivemongo.ConnectionListener {

  def poolCreated(
    options: MongoConnectionOptions,
    supervisor: String,
    connection: String): Unit = {

    val event = Event.builder().
      withHostname(hostname).
      withAggregationKey(supervisor).
      withSourceTypeName("reactivemongo").
      withAlertType(Event.AlertType.INFO).
      withDate(System.currentTimeMillis()).
      withTitle("reactivemongo.pool-created").
      withText(options.toString)

    client.recordEvent(
      event.build(),
      supervisor,
      s"${supervisor}.${connection}")
  }

  private val poolTags = MMap.empty[(String, String), Set[String]]

  private val nodes = MMap.empty[(String, String), Vector[String]]

  @com.github.ghik.silencer.silent("NodeSetInfo")
  def nodeSetUpdated(
    supervisor: String,
    connection: String,
    previous: NodeSetInfo,
    updated: NodeSetInfo): Unit = {

    val conPrefix = s"${supervisor}.${connection}"
    val prefix = updated.name.foldLeft(conPrefix) { (p, n) => s"$p.$n" }

    val key = supervisor -> connection
    val tagSet = Set(supervisor, conPrefix, prefix)

    poolTags.synchronized {
      poolTags.put(key, tagSet)
    }

    val tags = tagSet.toSeq

    traceUpdate(supervisor, tags, updated)

    val gauge = client.gauge(_: String, _: Long, tags: _*)

    // Node set gauges
    gauge(
      "awaitingRequests",
      updated.awaitingRequests.fold(0L)(_.toLong))

    gauge(
      "maxAwaitingRequestsPerChannel",
      updated.maxAwaitingRequestsPerChannel.fold(0L)(_.toLong))

    gauge("numberOfNodes", updated.nodes.size.toLong)

    gauge("hasPrimary", if (updated.primary.isDefined) 1L else 0L)

    gauge("hasNearest", if (updated.nearest.isDefined) 1L else 0L)

    gauge("isMongos", if (updated.mongos.isDefined) 1L else 0L)

    gauge("numberOfSecondaries", updated.secondaries.size.toLong)

    // Node detailed gauges
    nodes.synchronized {
      nodes.put(key, updated.nodes.map(_.name))
    }

    updated.nodes.foreach { node =>
      val nodeTags = tags :+ node.name

      def ng(property: String, value: Int) =
        client.gauge(property, value.toLong, nodeTags: _*)

      ng("nodeChannels", node.connections)

      ng("connectedChannels", node.connected)

      ng("authenticatedChannels", node.authenticated)

      client.gauge("pingTime", node.pingInfo.ping / 1000L, nodeTags: _*)
    }
  }

  def poolShutdown(supervisor: String, connection: String) = {
    val key = supervisor -> connection

    val pts = (poolTags.synchronized {
      poolTags.remove(key)
    }).getOrElse(Set.empty[String]).toSeq

    val reset = client.gauge(_: String, 0L, pts: _*)

    // Node set resetauresetes
    reset("awaitinresetRequests")
    reset("maxAwaitinresetRequestsPerChannel")
    reset("numberOfNodes")
    reset("hasPrimary")
    reset("hasNearest")
    reset("isMongos")
    reset("numberOfSecondaries")

    val ns = (nodes.synchronized {
      nodes.remove(key)
    }).getOrElse(Vector.empty[String])

    val conPrefix = s"${supervisor}.${connection}"
    val tagSet = Set(supervisor, conPrefix)

    ns.foreach { node =>
      val nts = (tagSet + node).toSeq

      val g = client.gauge(_: String, 0L, nts: _*)

      g("nodeChannels")
      g("connectedChannels")
      g("authenticatedChannels")
      g("pingTime")
    }

    val event = Event.builder().
      withHostname(hostname).
      withAggregationKey(supervisor).
      withSourceTypeName("reactivemongo").
      withAlertType(Event.AlertType.INFO).
      withDate(System.currentTimeMillis()).
      withTitle("pool-stopped").
      withText(ns mkString ", ")

    client.recordEvent(
      event.build(),
      supervisor,
      s"${supervisor}.${connection}")

    ()
  }

  @com.github.ghik.silencer.silent("NodeSetInfo")
  private def traceUpdate(
    supervisor: String,
    baseTags: Seq[String],
    updated: NodeSetInfo): Unit = {

    val event = Event.builder().
      withHostname(hostname).
      withAggregationKey(supervisor).
      withSourceTypeName("reactivemongo").
      withAlertType(Event.AlertType.INFO).
      withDate(System.currentTimeMillis()).
      withTitle("pool-updated").
      withText(updated.toString)

    val tags = Seq.newBuilder[String] ++= baseTags

    updated.version.foreach { v =>
      tags += s"version:$v"
    }

    updated.primary.foreach { node =>
      tags += s"primary:${node.name}"
    }

    updated.mongos.foreach { node =>
      tags += s"mongos:${node.name}"
    }

    updated.nearest.foreach { node =>
      tags += s"nearest:${node.name}"
    }

    client.recordEvent(event.build, tags.result(): _*)
  }
}
