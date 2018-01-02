package com.soundcloud.followermaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;

/**
 * Event receiver has one single function: to receive and acknowledge
 * events from an event source TCP connection as quickly as possible, and
 * enqueue them at an instance of {@link EventDispatcher} for asynchronous
 * processing and dispatching.
 */
class EventReceiver implements Runnable {

    private final static Logger auditLogger = Logger.getLogger("audit");
    private final static Logger errorLogger = Logger.getLogger("errors");

    private final Socket connection;
    private final EventDispatcher dispatcher;

    EventReceiver(Socket connection, EventDispatcher dispatcher) {
        this.connection = connection;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try {
            //
            // Please note that because we're buffering the socket's input
            // stream, each event source's TCP connection can be processed
            // by just one thread at a time.
            //
            BufferedReader in = bufferedReaderFrom(connection);
            String payload;
            while ((payload = in.readLine()) != null) {
                auditLogger.info("Received event: " + payload);
                Event event = Event.fromPayload(payload);
                dispatcher.submit(event);
            }
        } catch (IOException e) {
            errorLogger.warning("I/O error while receiving event: " + e.getMessage());
        }
    }
}
