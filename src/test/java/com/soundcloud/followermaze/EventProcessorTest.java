package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.soundcloud.followermaze.TestFixtures.buildEvents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class EventProcessorTest {

    private static final int QUEUE_SIZE = 2048;
    private static final int BATCH_SIZE = 1024;

    private EventRouter router;
    private ExecutorService executor;

    @Before
    public void setup() {
        router = mock(EventRouter.class);
        executor = Executors.newSingleThreadExecutor();
    }

    private EventProcessor buildProcessor(long timeout) {
        return new EventProcessor(router, QUEUE_SIZE, BATCH_SIZE, timeout);
    }

    @Test
    public void doNotDispatchEventsIfBatchIsNotFilled() throws Exception {
        EventProcessor processor = buildProcessor(Long.MAX_VALUE);
        executor.submit(processor);

        buildEvents(BATCH_SIZE - 1).forEach(processor::submit);

        executor.awaitTermination(10, TimeUnit.SECONDS);

        verify(router, never()).route(any(Event.class));
    }

    @Test
    public void dispatchEventsInOrderIfBatchIsFilled() throws Exception {
        EventProcessor processor = buildProcessor(Long.MAX_VALUE);
        executor.submit(processor);

        List<Event> events = buildEvents(BATCH_SIZE);
        Collections.shuffle(events);
        events.forEach(processor::submit);

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
    public void dispatchEventsInOrderIfBatchIsExceededAndKeepExcessInQueue() throws Exception {
        EventProcessor processor = buildProcessor(Long.MAX_VALUE);
        executor.submit(processor);

        List<Event> events = buildEvents(BATCH_SIZE + 1);
        Collections.shuffle(events);
        events.forEach(processor::submit);

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
    public void dispatchEventsInOrderIfTimeoutExpired() throws Exception {
        final long timeout = 5000;
        EventProcessor processor = buildProcessor(timeout);
        executor.submit(processor);

        List<Event> events = buildEvents(BATCH_SIZE - 1);
        Collections.shuffle(events);
        events.forEach(processor::submit);

        executor.awaitTermination(timeout * 2, TimeUnit.MILLISECONDS);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(router, times(events.size())).route(captor.capture());

        List<Event> routedEvents = captor.getAllValues();
        assertThat(routedEvents.size(), is(events.size()));

        IntStream.range(0, events.size()).forEach(
                i -> assertThat(routedEvents.get(i).getSequence(), is(i + 1L))
        );
    }
}
