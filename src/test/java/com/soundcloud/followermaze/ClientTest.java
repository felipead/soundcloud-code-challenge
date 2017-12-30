package com.soundcloud.followermaze;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;
import static com.soundcloud.followermaze.TestFixtures.buildEvents;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class ClientTest extends AbstractSocketServerTest {

    @Test
    public void sendOneEvent() throws IOException {
        final String payload = "666|F|60|50";
        Client client = new Client(666L, clientConnection);
        client.send(Event.fromPayload(payload));

        BufferedReader in = bufferedReaderFrom(serverConnection);
        assertThat(in.readLine(), equalTo(payload));
    }

    @Test
    public void sendSeveralEvents() throws IOException {
        final int numberOfEvents = 10000;
        final List<Event> events = buildEvents(numberOfEvents);

        Client client = new Client(666L, clientConnection);

        for (Event e : events) {
            client.send(e);
        }

        BufferedReader in = bufferedReaderFrom(serverConnection);
        for (Event e : events) {
            assertThat(in.readLine(), equalTo(e.toPayload()));
        }
    }
}
