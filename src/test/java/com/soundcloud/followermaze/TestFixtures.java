package com.soundcloud.followermaze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

final class TestFixtures {

    private static Event buildAssortedEvent(long sequence) {
        int type = new Long(sequence % 5).intValue();
        switch (type) {
            case 0: return Event.newFollow(sequence, 50L, 60L);
            case 1: return Event.newUnfollow(sequence, 50L, 60L);
            case 2: return Event.newBroadcast(sequence);
            case 3: return Event.newPrivateMessage(sequence, 50L, 60L);
            default: return Event.newStatusUpdate(sequence, 50L);
        }
    }

    static List<Event> buildEvents(long numberOfEvents) {
        return buildEvents(numberOfEvents, 1L);
    }

    private static List<Event> buildEvents(long numberOfEvents, long baseSequence) {
        return LongStream.range(baseSequence, baseSequence + numberOfEvents)
                .mapToObj(TestFixtures::buildAssortedEvent)
                .collect(Collectors.toList());
    }

    static List<Event> buildShuffledEvents(long numberOfEvents) {
        return buildShuffledEvents(numberOfEvents, 1L);
    }

    private static List<Event> buildShuffledEvents(long numberOfEvents, long baseSequence) {
        List<Event> events = buildEvents(numberOfEvents, baseSequence);
        Collections.shuffle(events);
        return events;
    }

    static List<Event> buildShuffledEventBatches(int batchSize, int numberOfBatches, int excessSize) {
        List<Event> batches = new ArrayList<>(numberOfBatches + 1);
        for (int i = 0; i < numberOfBatches; i++) {
            batches.addAll(buildShuffledEvents(batchSize, batchSize * i + 1));
        }
        batches.addAll(buildShuffledEvents(excessSize, batchSize * numberOfBatches + 1));
        return batches;
    }
}
