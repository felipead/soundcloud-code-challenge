package com.soundcloud.followermaze;

import java.util.concurrent.PriorityBlockingQueue;

public class EventProcessor implements Runnable {

    private final EventRouter router;

    // A thread-safe queue that stores comparable elements and retrieves then in order.
    private final PriorityBlockingQueue<Event> queue;

    private final int batchSize;
    private final long batchTimeout;
    private final Object dispatchMonitor;

    EventProcessor(EventRouter router) {
        this(router, 8192, 5000);
    }

    EventProcessor(EventRouter router, int batchSize, long batchTimeout) {
        this.router = router;
        this.queue = new PriorityBlockingQueue<>(batchSize * 2);
        this.batchSize = batchSize;
        this.batchTimeout = batchTimeout;
        this.dispatchMonitor = new Object();
    }

    public void submit(Event event) {
        queue.add(event);
        if (queue.size() >= batchSize) {
            synchronized (dispatchMonitor) {
                dispatchMonitor.notifyAll();
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (dispatchMonitor) {
                    dispatchMonitor.wait(batchTimeout);
                }
                dispatchBatch();
            } catch (InterruptedException ignored) { }
        }
    }

    private void dispatchBatch() {
        for (int i = 0; i < batchSize; i++) {
            Event e = queue.poll();
            if (e == null) break;
            router.route(e);
        }
    }
}
