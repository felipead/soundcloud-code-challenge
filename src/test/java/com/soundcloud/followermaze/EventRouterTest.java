package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

public class EventRouterTest {

    private EventRouter router;
    private AtomicLong sequence;

    @Before
    public void setup() {
        router = new EventRouter();
        sequence = new AtomicLong(999L);
    }

    private Socket nullSocket() {
        Socket socket = mock(Socket.class);
        try {
            when(socket.getOutputStream()).thenReturn(mock(OutputStream.class));
        } catch (IOException ignored) {}
        return socket;
    }

    private Socket bogusSocket() {
        Socket socket = mock(Socket.class);
        try {
            when(socket.getOutputStream()).thenThrow(IOException.class);
        } catch (IOException ignored) {}
        return socket;
    }

    private Client buildClient(long id) {
        return spy(new Client(id, nullSocket()));
    }

    private List<Client> buildClients(long startIdInclusive, long endIdExclusive) {
        return LongStream.range(startIdInclusive, endIdExclusive)
                .mapToObj(this::buildClient)
                .collect(Collectors.toList());
    }

    private void assertSentTo(Client client, Event event) {
        try { verify(client).send(event); } catch (IOException ignored) {}
    }

    private void assertNotSentTo(Client client, Event event) {
        try { verify(client, never()).send(event); } catch (IOException ignored) {}
    }

    @Test
    public void routeFollowEventWhenFolloweeExists() {
        Client follower = buildClient(60L);
        Client followee = buildClient(50L);
        List<Client> others = buildClients(1L, 10L);

        router.register(follower);
        router.register(followee);
        others.forEach(router::register);

        Event event = Event.newFollow(sequence.getAndIncrement(), follower.getId(), followee.getId());
        router.route(event);
        assertSentTo(followee, event);
        assertNotSentTo(follower, event);
        others.forEach(i -> assertNotSentTo(i, event));
    }

    @Test
    public void routeFollowEventWhenFolloweeDoesNotExist() {
        Client follower = buildClient(60L);
        router.register(follower);

        Event event = Event.newFollow(sequence.getAndIncrement(), follower.getId(), 99999L);
        router.route(event);
        assertNotSentTo(follower, event);
    }

    @Test
    public void routeUnfollowEventWhenFolloweeExists() {
        Client follower = buildClient(60L);
        Client followee = buildClient(50L);
        List<Client> others = buildClients(1L, 10L);

        router.register(follower);
        router.register(followee);
        others.forEach(router::register);

        Event event = Event.newUnfollow(sequence.getAndIncrement(), follower.getId(), followee.getId());
        router.route(event);

        assertNotSentTo(followee, event);
        assertNotSentTo(follower, event);
        others.forEach(i -> assertNotSentTo(i, event));
    }

    @Test
    public void routeUnfollowEventWhenFolloweeDoesNotExist() {
        Client follower = buildClient(60L);
        router.register(follower);

        Event event = Event.newUnfollow(sequence.getAndIncrement(), follower.getId(), 99999L);
        router.route(event);

        assertNotSentTo(follower, event);
    }

    @Test
    public void routeBroadcastEvent() {
        List<Client> clients = buildClients(1L, 10L);
        clients.forEach(router::register);

        Event event = Event.newBroadcast(666L);
        router.route(event);

        clients.forEach(i -> assertSentTo(i, event));
    }

    @Test
    public void routePrivateMessageEventWhenRecipientExists() {
        Client sender = buildClient(60L);
        Client recipient = buildClient(50L);
        List<Client> others = buildClients(1L, 10L);

        router.register(sender);
        router.register(recipient);
        others.forEach(router::register);

        Event event = Event.newPrivateMessage(sequence.getAndIncrement(), sender.getId(), recipient.getId());
        router.route(event);
        assertSentTo(recipient, event);
        assertNotSentTo(sender, event);
        others.forEach(i -> assertNotSentTo(i, event));
    }

    @Test
    public void routePrivateMessageEventWhenRecipientDoesNotExist() {
        Client sender = buildClient(60L);
        router.register(sender);

        Event event = Event.newPrivateMessage(sequence.getAndIncrement(), sender.getId(), 99999L);
        router.route(event);
        assertNotSentTo(sender, event);
    }

    @Test
    public void routeStatusUpdateWhenThereAreFollowers() {
        Client followee = buildClient(60L);
        Client other = buildClient(61L);

        List<Client> followers = buildClients(1L, 10L);
        List<Client> others = buildClients(10L, 20L);

        router.register(followee);
        router.register(other);
        followers.forEach(router::register);
        others.forEach(router::register);

        followers.forEach(i -> router.route(
                Event.newFollow(sequence.getAndIncrement(), i.getId(), followee.getId())));
        others.forEach(i -> router.route(
                Event.newFollow(sequence.getAndIncrement(), i.getId(), other.getId())));

        Event statusUpdate = Event.newStatusUpdate(sequence.getAndIncrement(), followee.getId());
        router.route(statusUpdate);

        followers.forEach(i -> assertSentTo(i, statusUpdate));
        others.forEach(i -> assertNotSentTo(i, statusUpdate));
        assertNotSentTo(followee, statusUpdate);
        assertNotSentTo(other, statusUpdate);
    }

    @Test
    public void routeStatusUpdateWhenThereAreNoFollowers() {
        Client followee = buildClient(60L);
        List<Client> others = buildClients(1L, 10L);

        router.register(followee);
        others.forEach(router::register);

        Event statusUpdate = Event.newStatusUpdate(sequence.getAndIncrement(), followee.getId());
        router.route(statusUpdate);

        assertNotSentTo(followee, statusUpdate);
        others.forEach(i -> assertNotSentTo(i, statusUpdate));
    }

    @Test
    public void routeStatusUpdateWhenThereAreUnfollowers() {
        Client followee = buildClient(60L);

        List<Client> followers = buildClients(1L, 10L);
        List<Client> unfollowers = buildClients(20L, 30L);
        List<Client> others = buildClients(10L, 20L);

        router.register(followee);
        followers.forEach(router::register);
        unfollowers.forEach(router::register);
        others.forEach(router::register);

        followers.forEach(i -> router.route(
                Event.newFollow(sequence.getAndIncrement(), i.getId(), followee.getId())));
        unfollowers.forEach(i -> {
            router.route(Event.newFollow(sequence.getAndIncrement(), i.getId(), followee.getId()));
            router.route(Event.newUnfollow(sequence.getAndIncrement(), i.getId(), followee.getId()));
        });

        Event statusUpdate = Event.newStatusUpdate(sequence.getAndIncrement(), followee.getId());
        router.route(statusUpdate);

        followers.forEach(i -> assertSentTo(i, statusUpdate));
        unfollowers.forEach(i -> assertNotSentTo(i, statusUpdate));
        others.forEach(i -> assertNotSentTo(i, statusUpdate));
        assertNotSentTo(followee, statusUpdate);
    }

    @Test
    public void routeStatusUpdateWhenFollowerDoesNotExist() {
        Client followee = buildClient(60L);
        router.register(followee);
        router.route(Event.newFollow(sequence.getAndIncrement(), 99999L, followee.getId()));

        Event statusUpdate = Event.newStatusUpdate(sequence.getAndIncrement(), followee.getId());
        router.route(statusUpdate);
    }

    @Test
    public void routeEventIgnoringSocketExceptions() {
        Client bogus = spy(new Client(1L, bogusSocket()));
        List<Client> others = buildClients(10L, 20L);

        router.register(bogus);
        others.forEach(router::register);

        Event broadcast = Event.newBroadcast(sequence.getAndIncrement());
        router.route(broadcast);

        assertSentTo(bogus, broadcast);
        others.forEach(i -> assertSentTo(i, broadcast));
    }

    @Test
    public void registerClientWithConcurrentThreads() throws InterruptedException, ExecutionException {
        final int numberOfThreads = 100;
        final int numberOfClients = 100000;

        List<Client> clients = buildClients(1L, numberOfClients + 1);
        BlockingQueue<Client> queue = new ArrayBlockingQueue<>(clients.size());
        queue.addAll(clients);

        Runnable task = () -> {
            while (true) {
                Client client = queue.poll();
                if (client == null) break;
                router.register(client);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<Future> promises = IntStream.range(0, numberOfThreads)
                .mapToObj(i -> executor.submit(task))
                .collect(Collectors.toList());

        for (Future promise : promises) {
            promise.get();
        }

        clients.forEach(i -> assertThat(router.getClient(i.getId()), equalTo(i)));
    }
}
