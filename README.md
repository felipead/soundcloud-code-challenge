# SoundCloud Follower Maze Code Submission

Problem description can be found [here](package/instructions.md).

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

## Regarding documentation

I'm a big fan of [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882). I strongly believe that code should be readable and document itself without the need of comments.

That's why I avoided writing obvious comments just for the sake of
it. Instead, I wrote comments only where I felt that they would
make a difference, in the form of high-level JavaDocs.

## Regarding tests

Most of the code is fully covered by some form of unit tests or white-box integration tests. The only place that is not tested is the Server class. Since this is the highest-level component, I'd rather have black-box integration tests or functional tests to cover it. Otherwise, I'd need to orchestrate several mocks in a complicated setup for a questionable gain.

If I had more time I would probably write those black-box tests using [Mountebank](http://www.mbtest.org) to record and [stub TCP iteractions](http://www.mbtest.org/docs/protocols/tcp).

In additional to functional tests, we also need load and performance tests. These can be made using [JMeter](http://jmeter.apache.org) or another tool that supports testing TCP sockets.

## Future improvements

If I had more time I would try to do the following:

- Instead of relying on an memory-based queue data structure, use a persistent message queue such as [RabbitMQ](https://www.rabbitmq.com), [ActiveMQ](http://activemq.apache.org) or [ØMQ](http://zeromq.org). This way, if the server dies and needs to be restarted, requests won't be lost.

- Instead of persisting the follow/unfollow status in a memory-based data structure, store this relationship status in a persistent key-value store, such as [Redis](https://redis.io). This way, if the server dies and needs to be restarted, this information won't be lost.

- Run the server in a container (eg: [Docker](https://www.docker.com)). Orchestrate possible dependencies (eg: Redis, RabbitMQ) using [docker-compose](https://docs.docker.com/compose/) or a similar tool.

- Improve monitoring (for instance, by adding a health status check).
