package com.soundcloud.followermaze;

import java.util.regex.Pattern;

public class Event implements Comparable<Event> {

    private long sequence;
    private EventType type;
    private Long fromId;
    private Long toId;

    private Event(long sequence, EventType type, Long fromId, Long toId) {
        this.sequence = sequence;
        this.type = type;
        this.fromId = fromId;
        this.toId = toId;
    }

    public static Event newFollow(long sequence, long fromId, long toId) {
        return new Event(sequence, EventType.FOLLOW, fromId, toId);
    }

    public static Event newUnfollow(long sequence, long fromId, long toId) {
        return new Event(sequence, EventType.UNFOLLOW, fromId, toId);
    }

    public static Event newBroadcast(long sequence) {
        return new Event(sequence, EventType.BROADCAST, null, null);
    }

    public static Event newPrivateMessage(long sequence, long fromId, long toId) {
        return new Event(sequence, EventType.PRIVATE_MESSAGE, fromId, toId);
    }

    public static Event newStatusUpdate(long sequence, long fromId) {
        return new Event(sequence, EventType.STATUS_UPDATE, fromId, null);
    }

    public static Event fromPayload(String payload) {
        String[] tokens = getTokens(payload);
        EventType type = EventType.fromCode(tokens[1]);

        return new Event(
                Long.parseLong(tokens[0]),
                type,
                type.hasFrom()? Long.parseLong(tokens[2]) : null,
                type.hasTo()? Long.parseLong(tokens[3]) : null
        );
    }

    private static String[] getTokens(String payload) {
        String[] tokens = payload.split(Pattern.quote("|"));
        assert(tokens.length >= 2);
        return tokens;
    }

    public String toPayload() {
        StringBuilder s = new StringBuilder();
        s.append(sequence).append('|');
        s.append(type.getCode());
        if (type.hasFrom()) {
            s.append('|').append(fromId);
        }
        if (type.hasTo()) {
            s.append('|').append(toId);
        }
        return s.toString();
    }

    public long getSequence() {
        return sequence;
    }

    public EventType getType() {
        return type;
    }

    public Long getFromId() {
        return fromId;
    }

    public Long getToId() {
        return toId;
    }

    @Override
    public int compareTo(Event o) {
        return Long.compare(sequence, o.sequence);
    }
}
