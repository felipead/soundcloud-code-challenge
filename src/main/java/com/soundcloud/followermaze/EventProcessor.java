package com.soundcloud.followermaze;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EventProcessor implements Runnable {

    private static final int DEFAULT_ROUTING_WINDOW = 10240;
    private static final int DEFAULT_QUEUE_CAPACITY = DEFAULT_ROUTING_WINDOW * 2;

    private final EventRouter router;
    private final BlockingQueue<Event> queue;
    private final int routingWindow;

    EventProcessor(EventRouter router) {
        this(router, DEFAULT_QUEUE_CAPACITY, DEFAULT_ROUTING_WINDOW);
    }

    EventProcessor(EventRouter router, int queueCapacity, int routingWindow) {
        this.router = router;
        this.queue = new PriorityBlockingQueue<>(queueCapacity);
        this.routingWindow = routingWindow;
    }

    public void submit(Event event) {
        queue.add(event);
    }

    @Override
    public void run() {
        while (true) {
            if (queue.size() >= routingWindow) {
                List<Event> events = IntStream.range(0, routingWindow)
                        .mapToObj(i -> queue.remove())
                        .collect(Collectors.toList());
                router.route(events);
            }
        }
    }
}
