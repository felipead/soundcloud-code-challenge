package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.soundcloud.followermaze.TestFixtures.buildEvents;
import static com.soundcloud.followermaze.TestFixtures.buildShuffledEvents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;


// I know inserting timeouts in tests is not a good idea, as
// it makes tests slow and flaky under certain conditions. However,
// I couldn't find any simple way to synchronize producer/consumer threads
// during tests without significantly changing the design of the code
// being tested. This should be improved if it becomes an issue.


public class EventProcessorTest {

    private static final int BATCH_SIZE = 1024;

    private EventRouter router;
    private ExecutorService executor;

    @Before
    public void setup() {
        router = mock(EventRouter.class);
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void doesNotDispatchEventsIfBatchIsNotFilledAndTimeoutIsNotExpired() throws Exception {
        final EventProcessor processor = new EventProcessor(router, BATCH_SIZE, Long.MAX_VALUE);

        executor.submit(processor);
        buildEvents(BATCH_SIZE - 1).forEach(processor::submit);
        executor.awaitTermination(10, TimeUnit.SECONDS);

        verify(router, never()).route(any(Event.class));
    }

    @Test
    public void dispatchesEventsInOrderIfBatchIsFilled() throws Exception {
        final EventProcessor processor = new EventProcessor(router, BATCH_SIZE, Long.MAX_VALUE);

        executor.submit(processor);
        buildShuffledEvents(BATCH_SIZE).forEach(processor::submit);
        executor.awaitTermination(10, TimeUnit.SECONDS);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(BATCH_SIZE)).route(captor.capture());

        List<Event> routedEvents = captor.getAllValues();
        assertThat(routedEvents.size(), is(BATCH_SIZE));

        IntStream.range(0, BATCH_SIZE).forEach(
                i -> assertThat(routedEvents.get(i).getSequence(), is(i + 1L))
        );
    }

    @Test
    public void dispatchesEventsInOrderIfBatchIsExceededAndKeepRemainingEventsInQueue() throws Exception {
        final EventProcessor processor = new EventProcessor(router, BATCH_SIZE, Long.MAX_VALUE);

        executor.submit(processor);
        buildShuffledEvents(BATCH_SIZE + 1).forEach(processor::submit);
        executor.awaitTermination(10, TimeUnit.SECONDS);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(BATCH_SIZE)).route(captor.capture());

        List<Event> routedEvents = captor.getAllValues();
        assertThat(routedEvents.size(), is(BATCH_SIZE));

        IntStream.range(0, BATCH_SIZE).forEach(
                i -> assertThat(routedEvents.get(i).getSequence(), is(i + 1L))
        );
    }

    @Test
    public void dispatchesEventsInOrderIfTimeoutExpiredAndBatchIsNotFilled() throws Exception {
        final long batchTimeout = 5000;
        final int numberOfEvents = BATCH_SIZE - 1;
        final EventProcessor processor = new EventProcessor(router, BATCH_SIZE, batchTimeout);

        executor.submit(processor);
        buildShuffledEvents(numberOfEvents).forEach(processor::submit);
        executor.awaitTermination(batchTimeout * 2, TimeUnit.MILLISECONDS);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(numberOfEvents)).route(captor.capture());

        List<Event> routedEvents = captor.getAllValues();
        assertThat(routedEvents.size(), is(numberOfEvents));

        IntStream.range(0, numberOfEvents).forEach(
                i -> assertThat(routedEvents.get(i).getSequence(), is(i + 1L))
        );
    }

    @Test
    public void dispatchesAllEventsEventually() throws Exception {
        final long batchTimeout = 5000;
        final int excessSize = 100;
        final int numberOfEvents = BATCH_SIZE * 2 + excessSize;
        final EventProcessor processor = new EventProcessor(router, BATCH_SIZE, batchTimeout);

        buildShuffledEvents(BATCH_SIZE, 1).forEach(processor::submit);
        executor.submit(processor);
        buildShuffledEvents(BATCH_SIZE, BATCH_SIZE + 1).forEach(processor::submit);
        buildShuffledEvents(excessSize, (BATCH_SIZE * 2) + 1).forEach(processor::submit);
        executor.awaitTermination(batchTimeout * 3, TimeUnit.MILLISECONDS);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(numberOfEvents)).route(captor.capture());

        List<Event> routedEvents = captor.getAllValues();
        assertThat(routedEvents.size(), is(numberOfEvents));

        IntStream.range(0, numberOfEvents).forEach(
                i -> assertThat(routedEvents.get(i).getSequence(), is(i + 1L))
        );
    }
}
