# SoundCloud Follower Maze Code Submission

## Requirements

This solution was implemented using Java 8. It requires an *Oracle JDK version 1.8* or higher. It will not work with previous JDKs. It was not tested on Open JDK.

Please make sure your system points to the correct JDK by setting the value of the `$JAVA_HOME` environment variable.

On macOS, you can find the path of a specific JDK version using the `java_home` command:

```sh
/usr/libexec/java_home --version 1.8
```

## Starting the server

```sh
./run.sh
```

The server will listen on ports 9090 and 9099.

## Running the test suite

```sh
./test.sh
```

## Future improvements

If I had more time I would try to do the following:

- Write automated high-level integration and functional tests. Use [Mountebank](http://www.mbtest.org) to record and [stub TCP iteractions](http://www.mbtest.org/docs/protocols/tcp).

- Write automated load and performance tests. Probably using [JMeter](http://jmeter.apache.org) or another tool that supports testing TCP sockets.

- Instead of relying on an memory-based queue data structure, use a persistent message queue such as [RabbitMQ](https://www.rabbitmq.com), [ActiveMQ](http://activemq.apache.org) or [ØMQ](http://zeromq.org). This way, if the server dies and needs to be restarted, requests won't be lost.

- Instead of persisting the follow/unfollow status in a memory-based data structure, store this relationship status in a persistent key-value store, such as [Redis](https://redis.io). This way, if the server dies and needs to be restarted, this information won't be lost.

- Run the server in a container (eg: [Docker](https://www.docker.com)). Orchestrate possible dependencies (eg: Redis, RabbitMQ) using [docker-compose](https://docs.docker.com/compose/) or a similar tool.

- Write documentation describing the high-level architecture. Right now, there's none. Sorry about that folks!
