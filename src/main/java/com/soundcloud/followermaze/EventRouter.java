package com.soundcloud.followermaze;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EventRouter {

    private final static int INITIAL_CLIENT_CAPACITY = 100;

    private final static Logger auditLogger = Logger.getLogger("audit");
    private final static Logger errorLogger = Logger.getLogger("errors");

    private final Map<Long, Client> clients = new ConcurrentHashMap<>(INITIAL_CLIENT_CAPACITY);
    private final Map<Long, Set<Long>> followers = new HashMap<>();

    public void register(Client client) {
        clients.put(client.getId(), client);
    }

    public void route(Event event) {
        switch (event.getType()) {
            case FOLLOW:
                addFollower(event.getFromId(), event.getToId());
                send(event.getToId(), event);
                break;
            case UNFOLLOW:
                removeFollower(event.getFromId(), event.getToId());
                break;
            case BROADCAST:
                clients.values().forEach(i -> send(i, event));
                break;
            case PRIVATE_MESSAGE:
                send(event.getToId(), event);
                break;
            case STATUS_UPDATE:
                sendToFollowers(event.getFromId(), event);
                break;
        }
    }

    protected Client getClient(Long id) {
        return clients.get(id);
    }

    private void addFollower(Long followerId, Long followeeId) {
        if (!followers.containsKey(followeeId)) {
            followers.put(followeeId, new HashSet<>());
        }
        followers.get(followeeId).add(followerId);
    }

    private void removeFollower(Long followerId, Long followeeId) {
        if (followers.containsKey(followeeId)) {
            followers.get(followeeId).remove(followerId);
        }
    }

    private void sendToFollowers(Long followeeId, Event event) {
        if (followers.containsKey(followeeId)) {
            followers.get(followeeId).forEach(i -> send(i, event));
        }
    }

    private void send(Long recipientId, Event event) {
        Client recipient = clients.get(recipientId);
        if (recipient != null) {
            send(recipient, event);
        }
    }

    private void send(Client recipient, Event event) {
        Long id = recipient.getId();
        try {
            auditLogger.info(String.format("Forwarding to client %d event: %s", id, event.toPayload()));
            recipient.send(event);
        } catch (IOException e) {
            errorLogger.warning(String.format("I/O error while forwarding event to client %d: %s", id, e.getMessage()));
        }
    }
}
