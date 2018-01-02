package com.soundcloud.followermaze;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Event dispatcher is an event loop. It has an internal queue that synchronize
 * producers and consumers of events. Producers can submit events at will.
 *
 * At each interaction, events are picked from the queue in batches, and then
 * dispatched to {@link EventRouter}. If an event stays in the queue for more
 * than a timeout period, it will be dispatched regardless of a batch being
 * filled or not.
 *
 * The queue also sorts events by their respective sequence numbers, in case events
 * are sent out-of-order. Please note that the ability for this server to sort
 * out-of-order events is dictated by the batch size and batch timeout values.
 * They should be large enough to fit your needs while not sacrificing
 * efficiency.
 *
 * When implementing TCP servers, it is important to receive and acknowledge
 * requests as quickly as possible to achieve maximum server throughput and
 * utilize the network efficiently.
 *
 * Processing of requests can be done later, asynchronously, at the speed the
 * server supports. That's what this class is for.
 */
public class EventDispatcher implements Runnable {

    private final EventRouter router;

    // A thread-safe queue that stores comparable elements and retrieves then in order.
    private final PriorityBlockingQueue<Event> queue;

    private final int batchSize;
    private final long batchTimeout;
    private final Object dispatchMonitor;

    EventDispatcher(EventRouter router) {
        this(router, 8192, 5000);
    }

    EventDispatcher(EventRouter router, int batchSize, long batchTimeout) {
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
