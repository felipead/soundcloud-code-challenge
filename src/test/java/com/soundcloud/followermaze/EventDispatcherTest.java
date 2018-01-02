package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.soundcloud.followermaze.TestFixtures.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;


//
// I know inserting timeouts in tests is not a good idea, as
// it makes tests slow and flaky under certain conditions. However,
// I couldn't find any simple way to synchronize producer/consumer threads
// during tests without significantly changing the design of the code
// being tested. This should be improved if it becomes an issue.
//

public class EventDispatcherTest {

    private static final int BATCH_SIZE = 1024;

    private EventRouter router;
    private ExecutorService dispatcherExecutor;

    @Before
    public void setup() {
        router = mock(EventRouter.class);
        dispatcherExecutor = Executors.newSingleThreadExecutor();
    }

    private void assertEventsOrderedSequentially(List<Event> events) {
        IntStream.range(0, events.size()).forEach(
                i -> assertThat(events.get(i).getSequence(), is(i + 1L))
        );
    }

    private List<Event> assertEventsDispatched(int expectedNumberOfDispatchedEvents) {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(expectedNumberOfDispatchedEvents)).route(captor.capture());
        return captor.getAllValues();
    }

    @Test
    public void doesNotDispatchEventsIfBatchIsNotFilledAndTimeoutIsNotExpired() throws Exception {
        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, Long.MAX_VALUE);

        dispatcherExecutor.submit(dispatcher);
        buildEvents(BATCH_SIZE - 1).forEach(dispatcher::submit);
        dispatcherExecutor.awaitTermination(10, TimeUnit.SECONDS);

        verify(router, never()).route(any(Event.class));
    }

    @Test
    public void dispatchesEventsInOrderIfBatchIsFilled() throws Exception {
        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, Long.MAX_VALUE);

        dispatcherExecutor.submit(dispatcher);
        buildShuffledEvents(BATCH_SIZE).forEach(dispatcher::submit);
        dispatcherExecutor.awaitTermination(10, TimeUnit.SECONDS);

        List<Event> dispatchedEvents = assertEventsDispatched(BATCH_SIZE);
        assertEventsOrderedSequentially(dispatchedEvents);
    }

    @Test
    public void dispatchesEventsInOrderIfBatchIsExceededAndKeepRemainingEventsInQueue() throws Exception {
        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, Long.MAX_VALUE);

        dispatcherExecutor.submit(dispatcher);
        buildShuffledEvents(BATCH_SIZE + 1).forEach(dispatcher::submit);
        dispatcherExecutor.awaitTermination(10, TimeUnit.SECONDS);

        List<Event> dispatchedEvents = assertEventsDispatched(BATCH_SIZE);
        assertThat(dispatchedEvents.size(), is(BATCH_SIZE));
        assertEventsOrderedSequentially(dispatchedEvents);
    }

    @Test
    public void dispatchesEventsInOrderIfTimeoutExpiredAndBatchIsNotFilled() throws Exception {
        final long batchTimeout = 5000;
        final int numberOfEvents = BATCH_SIZE - 1;
        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, batchTimeout);

        dispatcherExecutor.submit(dispatcher);
        buildShuffledEvents(numberOfEvents).forEach(dispatcher::submit);
        dispatcherExecutor.awaitTermination(batchTimeout * 2, TimeUnit.MILLISECONDS);

        List<Event> dispatchedEvents = assertEventsDispatched(numberOfEvents);
        assertEventsOrderedSequentially(dispatchedEvents);
    }

    @Test
    public void dispatchesAllEventsEventuallyWithEventsGeneratedBySingleProducer() throws Exception {
        final int excessSize = 100;
        final int numberOfBatches = 10;
        final int numberOfEvents = BATCH_SIZE * numberOfBatches + excessSize;
        final long batchTimeout = 1000;
        final long dispatcherTimeout = batchTimeout * (numberOfBatches + 1);

        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, batchTimeout);
        dispatcherExecutor.submit(dispatcher);
        buildShuffledEventBatches(BATCH_SIZE, numberOfBatches, excessSize).forEach(dispatcher::submit);
        dispatcherExecutor.awaitTermination(dispatcherTimeout, TimeUnit.MILLISECONDS);

        List<Event> dispatchedEvents = assertEventsDispatched(numberOfEvents);
        assertEventsOrderedSequentially(dispatchedEvents);
    }


    //
    // The ability for multiple concurrent threads to submit events is not needed, since we only
    // have one event receiver thread to serve a single event source TCP connection. However, if we
    // ever need to serve multiple event sources and serializing all their events in one queue, this
    // implementation already supports it. I'm not a fan of over-engineering, but I just thought it
    // would be fun to test this scenario :D
    //

    @Test
    public void dispatchesAllEventsEventuallyWithEventsGeneratedByMultipleConcurrentProducers() throws Exception {
        final int numberOfProducerThreads = 100;
        final int excessSize = 100;
        final int numberOfBatches = 10;
        final int numberOfEvents = BATCH_SIZE * numberOfBatches + excessSize;
        final long batchTimeout = 1000;
        final long dispatcherTimeout = batchTimeout * (numberOfBatches + 1);

        final EventDispatcher dispatcher = new EventDispatcher(router, BATCH_SIZE, batchTimeout);
        dispatcherExecutor.submit(dispatcher);

        BlockingQueue<Event> queue = new ArrayBlockingQueue<>(numberOfEvents);
        queue.addAll(buildShuffledEventBatches(BATCH_SIZE, numberOfBatches, excessSize));

        Runnable producerTask = () -> {
            while (true) {
                Event e = queue.poll();
                if (e == null) break;
                dispatcher.submit(e);
            }
        };

        ExecutorService producerExecutor = Executors.newFixedThreadPool(numberOfProducerThreads);
        List<Future> producerPromises = IntStream.range(0, numberOfProducerThreads)
                .mapToObj(i -> producerExecutor.submit(producerTask))
                .collect(Collectors.toList());

        for (Future promise : producerPromises) {
            promise.get();
        }

        dispatcherExecutor.awaitTermination(dispatcherTimeout, TimeUnit.MILLISECONDS);

        List<Event> dispatchedEvents = assertEventsDispatched(numberOfEvents);
        assertEventsOrderedSequentially(dispatchedEvents);
    }
}
