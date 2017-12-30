package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.soundcloud.followermaze.TestFixtures.buildEvents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class EventProcessorTest {
    // TODO: receives events, buffer is not filled yet, buffer timeout is not expired -> does nothing
    // TODO: receives events, buffer is filled, buffer timeout is not expired -> batch route events
    // TODO: receives events, buffer is not filled yet, buffer timeout is expired -> batch route events

    private EventRouter router;
    private ExecutorService executor;

    @Before
    public void setup() {
        router = mock(EventRouter.class);
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    public void doNotRouteEventsIfWindowIsNotFilledAndTimeoutIsNotExpired() throws Exception {
        final int queueCapacity = 2048;
        final int routingWindow = 1024;

        EventProcessor processor = new EventProcessor(router, queueCapacity, routingWindow);
        executor.submit(processor);

        buildEvents(routingWindow - 1).forEach(processor::submit);
        executor.awaitTermination(30, TimeUnit.SECONDS);

        verify(router, never()).route(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void routeEventsIfWindowIsExactlyFilledAndTimeoutIsNotExpired() throws Exception {
        final int queueCapacity = 2048;
        final int routingWindow = 1024;

        EventProcessor processor = new EventProcessor(router, queueCapacity, routingWindow);
        executor.submit(processor);

        List<Event> events = buildEvents(routingWindow);
        Collections.shuffle(events);
        events.forEach(processor::submit);

        executor.awaitTermination(30, TimeUnit.SECONDS);

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(router).route(captor.capture());

        List<Event> routedEvents = captor.getValue();
        assertThat(routedEvents.size(), is(routingWindow));
        for (int i = 0; i < routingWindow; i++) {
            assertThat(routedEvents.get(i).getSequence(), is(i + 1L));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void routeEventsIfWindowIsExceededAndTimeoutIsNotExpired() throws Exception {
        final int queueCapacity = 2048;
        final int routingWindow = 1024;

        EventProcessor processor = new EventProcessor(router, queueCapacity, routingWindow);
        executor.submit(processor);

        List<Event> events = buildEvents(routingWindow + 1);
        Collections.shuffle(events);
        events.forEach(processor::submit);

        executor.awaitTermination(30, TimeUnit.SECONDS);

        ArgumentCaptor<List<Event>> captor = ArgumentCaptor.forClass(List.class);
        verify(router).route(captor.capture());

        List<Event> routedEvents = captor.getValue();
        assertThat(routedEvents.size(), is(routingWindow));
        for (int i = 0; i < routingWindow; i++) {
            assertThat(routedEvents.get(i).getSequence(), is(i + 1L));
        }
    }
}
