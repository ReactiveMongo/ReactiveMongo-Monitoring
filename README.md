# ReactiveMongo Monitoring

Monitoring & Instrumentation for ReactiveMongo

## JMX

This library expose ReactiveMongo metrics through [JMX](https://en.wikipedia.org/wiki/Java_Management_Extensions).

```ocaml
libraryDependencies += "org.reactivemongo" %% "reactivemongo-jmx" % VERSION
```

**See [documentation](http://reactivemongo.org/releases/0.1x/documentation/advanced-topics/monitoring.html#jmx)**

## Kamon

This library expose ReactiveMongo metrics using [Kamon](https://kamon.io).

```ocaml
libraryDependencies += "org.reactivemongo" %% "reactivemongo-kamon" % VERSION
```

**Pool metrics:** (tagged by pool name)

- `reactivemongo.awaitingRequests`: Total number of requests awaiting to be processed by MongoDB nodes.
- `reactivemongo.maxAwaitingRequestsPerChannel`: Maximum number of requests that were awaiting to be processed for a single channel (see `maxInFlightRequestsPerChannel`).
- `reactivemongo.numberOfNodes`: Number of nodes that are part of the set.
- `reactivemongo.hasPrimary`: Indicates whether the primary node is known (0 or 1).
- `reactivemongo.hasNearest`: Indicates whether the nearest node is known (0 or 1).
- `reactivemongo.isMongos`: Indicates whether mongos is used (0 or 1).
- `reactivemongo.numberOfSecondaries`: Number of secondary nodes in the set.

**Node metrics:** (tagged by pool & node)

- `reactivemongo.nodeChannels`: Number of network channels to a same MongoDB node, whatever is the status (see `connectedChannels`).
- `reactivemongo.connectedChannels`: Number of connected channels to a same MongoDB node (see `nodeChannels`).
- `reactivemongo.authenticatedChannels`: Number of authenticated channels to a same MongoDB node (see `connectedChannels`).
- `reactivemongo.pingTime`: Response delay (in nanoseconds) for the last IsMaster request.

## Build manually

ReactiveMongo BSON libraries can be built from this source repository.

    sbt publishLocal

To run the tests, use:

    sbt test

[Travis](https://travis-ci.org/ReactiveMongo/ReactiveMongo-Monitoring): ![Travis build status](https://travis-ci.org/ReactiveMongo/ReactiveMongo-Monitoring.png?branch=master)
