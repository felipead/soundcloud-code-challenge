package com.soundcloud.followermaze;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;

public class EventProcessor implements Runnable {

    private final EventRouter router;
    private final BlockingQueue<Event> queue;
    private final int batchSize;
    private final long timeoutMilliseconds;

    EventProcessor(EventRouter router) {
        this(router, 20480, 10240, 5000);
    }

    EventProcessor(EventRouter router, int queueSize, int batchSize, long timeoutMilliseconds) {
        this.router = router;
        this.queue = new PriorityBlockingQueue<>(queueSize);
        this.batchSize = batchSize;
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public void submit(Event event) {
        queue.add(event);
    }

    @Override
    public void run() {
        long lastDispatch = currentTimeMillis();
        while (true) {
            if (queue.size() >= batchSize) {
                dispatchBatch();
                lastDispatch = currentTimeMillis();
            }
            else if (timeoutExpiredSince(lastDispatch)) {
                dispatchRemaining();
                lastDispatch = currentTimeMillis();
            }
        }
    }

    private void dispatchRemaining() {
        while (!queue.isEmpty()) {
            router.route(queue.remove());
        }
    }

    private void dispatchBatch() {
        IntStream.range(0, batchSize)
                .forEach(i -> router.route(queue.remove()));
    }

    private boolean timeoutExpiredSince(long lastDispatch) {
        return (currentTimeMillis() - lastDispatch) >= timeoutMilliseconds;
    }
}
