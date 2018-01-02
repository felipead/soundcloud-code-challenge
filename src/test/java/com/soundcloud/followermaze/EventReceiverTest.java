package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.Writer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.soundcloud.followermaze.SocketUtils.bufferedWriterFrom;
import static com.soundcloud.followermaze.TestFixtures.buildEvents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class EventReceiverTest extends AbstractSocketServerTest {

    private EventDispatcher eventDispatcher;

    @Before
    public void setup() {
        eventDispatcher = mock(EventDispatcher.class);
    }

    @Test
    public void forwardsOneEventToDispatcher() throws Exception {
        final String payload = "666|F|60|50";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new EventReceiver(clientConnection, eventDispatcher));

        Writer out = bufferedWriterFrom(serverConnection);
        out.write(payload);
        out.write("\r\n");
        out.flush();

        serverConnection.close();
        promise.get();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher).submit(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat(event.toPayload(), is(payload));
    }

    @Test
    public void forwardsSeveralEventsToDispatcherOneByOne() throws Exception {
        final int numberOfEvents = 100000;
        final List<Event> events = buildEvents(numberOfEvents);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new EventReceiver(clientConnection, eventDispatcher));

        Writer out = bufferedWriterFrom(serverConnection);
        for (Event event : events) {
            out.write(event.toPayload());
            out.write("\r\n");
        }
        out.flush();

        serverConnection.close();
        promise.get();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventDispatcher, times(numberOfEvents)).submit(eventCaptor.capture());

        List<Event> sentEvents = eventCaptor.getAllValues();
        for (int i = 0; i < numberOfEvents; i++) {
            assertThat(sentEvents.get(i).toPayload(), is(events.get(i).toPayload()));
        }
    }
}
