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

public class EventListenerTest extends AbstractSocketServerTest {

    private EventProcessor eventProcessor;

    @Before
    public void setup() {
        eventProcessor = mock(EventProcessor.class);
    }

    @Test
    public void forwardsOneEventToProcessor() throws Exception {
        final String payload = "666|F|60|50";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new EventListener(clientConnection, eventProcessor));

        Writer out = bufferedWriterFrom(serverConnection);
        out.write(payload);
        out.write("\r\n");
        out.flush();

        serverConnection.close();
        promise.get();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventProcessor).submit(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat(event.toPayload(), is(payload));
    }

    @Test
    public void forwardsSeveralEventsToProcessorOneByOne() throws Exception {
        final int numberOfEvents = 100000;
        final List<Event> events = buildEvents(numberOfEvents);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new EventListener(clientConnection, eventProcessor));

        Writer out = bufferedWriterFrom(serverConnection);
        for (Event event : events) {
            out.write(event.toPayload());
            out.write("\r\n");
        }
        out.flush();

        serverConnection.close();
        promise.get();

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventProcessor, times(numberOfEvents)).submit(eventCaptor.capture());

        List<Event> sentEvents = eventCaptor.getAllValues();
        for (int i = 0; i < numberOfEvents; i++) {
            assertThat(sentEvents.get(i).toPayload(), is(events.get(i).toPayload()));
        }
    }
}
