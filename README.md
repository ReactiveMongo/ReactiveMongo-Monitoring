# ReactiveMongo Monitoring

Monitoring & Instrumentation for ReactiveMongo

## JMX

This library expose ReactiveMongo metrics through [JMX](https://en.wikipedia.org/wiki/Java_Management_Extensions).

```ocaml
libraryDependencies += "org.reactivemongo" %% "reactivemongo-jmx" % VERSION
```

**See [documentation](http://reactivemongo.org/releases/0.1x/documentation/advanced-topics/monitoring.html#jmx)**

## Build manually

ReactiveMongo BSON libraries can be built from this source repository.

    sbt publishLocal

To run the tests, use:

    sbt test

[Travis](https://travis-ci.org/ReactiveMongo/ReactiveMongo-Monitoring): ![Travis build status](https://travis-ci.org/ReactiveMongo/ReactiveMongo-Monitoring.png?branch=master)
