package com.soundcloud.followermaze;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class EventTest {

    @Test
    public void fromFollowPayload() {
        String payload = "666|F|60|50";
        Event event = Event.fromPayload(payload);
        assertThat(event.getSequence(), is(666L));
        assertThat(event.getType(), is(EventType.FOLLOW));
        assertThat(event.getFromId(), is(60L));
        assertThat(event.getToId(), is(50L));
    }

    @Test
    public void toFollowPayload() {
        Event event = Event.newFollow(666L, 60L, 50L);
        assertThat(event.toPayload(), is("666|F|60|50"));
    }

    @Test
    public void fromUnfollowPayload() {
        String payload = "1|U|12|9";
        Event event = Event.fromPayload(payload);
        assertThat(event.getSequence(), is(1L));
        assertThat(event.getType(), is(EventType.UNFOLLOW));
        assertThat(event.getFromId(), is(12L));
        assertThat(event.getToId(), is(9L));
    }

    @Test
    public void toUnfollowPayload() {
        Event event = Event.newUnfollow(1L, 12L, 9L);
        assertThat(event.toPayload(), is("1|U|12|9"));
    }

    @Test
    public void fromBroadcastPayload() {
        String payload = "542532|B";
        Event event = Event.fromPayload(payload);
        assertThat(event.getSequence(), is(542532L));
        assertThat(event.getType(), is(EventType.BROADCAST));
        assertThat(event.getFromId(), is(nullValue()));
        assertThat(event.getToId(), is(nullValue()));
    }

    @Test
    public void toBroadcastPayload() {
        Event event = Event.newBroadcast(542532L);
        assertThat(event.toPayload(), is("542532|B"));
    }

    @Test
    public void fromPrivateMessagePayload() {
        String payload = "43|P|32|56";
        Event event = Event.fromPayload(payload);
        assertThat(event.getSequence(), is(43L));
        assertThat(event.getType(), is(EventType.PRIVATE_MESSAGE));
        assertThat(event.getFromId(), is(32L));
        assertThat(event.getToId(), is(56L));
    }

    @Test
    public void toPrivateMessagePayload() {
        Event event = Event.newPrivateMessage(43L, 32L, 56L);
        assertThat(event.toPayload(), is("43|P|32|56"));
    }

    @Test
    public void fromStatusUpdatePayload() {
        String payload = "634|S|32";
        Event event = Event.fromPayload(payload);
        assertThat(event.getSequence(), is(634L));
        assertThat(event.getType(), is(EventType.STATUS_UPDATE));
        assertThat(event.getFromId(), is(32L));
        assertThat(event.getToId(), is(nullValue()));
    }

    @Test
    public void toStatusUpdatePayload() {
        Event event = Event.newStatusUpdate(634L, 32L);
        assertThat(event.toPayload(), is("634|S|32"));
    }

    @Test
    public void compareBySequenceNumber_lessThan() {
        int comparison = Event.newBroadcast(1L).compareTo(Event.newBroadcast(2L));
        assertThat(comparison, is(-1));
    }

    @Test
    public void compareBySequenceNumber_greaterThan() {
        int comparison = Event.newBroadcast(2L).compareTo(Event.newBroadcast(1L));
        assertThat(comparison, is(+1));
    }

    @Test
    public void compareBySequenceNumber_equality() {
        int comparison = Event.newBroadcast(10L).compareTo(Event.newBroadcast(10L));
        assertThat(comparison, is(0));
    }

    @Test
    public void sortBySequenceNumber() {
        List<Event> events = Stream.of(
                "542532|B",
                "2|U|12|9",
                "637|S|32",
                "1|F|12|9",
                "666|F|60|50",
                "43|F|32|56",
                "634|S|32",
                "1000|F|60|50",
                "666|B",
                "60|P|32|56"
        ).map(Event::fromPayload).collect(Collectors.toList());

        assertThat(
                events.stream().sorted().map(Event::toPayload).collect(Collectors.toList()),
                equalTo(Arrays.asList(
                        "1|F|12|9",
                        "2|U|12|9",
                        "43|F|32|56",
                        "60|P|32|56",
                        "634|S|32",
                        "637|S|32",
                        "666|F|60|50",
                        "666|B",
                        "1000|F|60|50",
                        "542532|B"
                ))
        );
    }
}
