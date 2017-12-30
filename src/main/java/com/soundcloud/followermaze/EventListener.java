package com.soundcloud.followermaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;

class EventListener implements Runnable {

    private final static Logger auditLogger = Logger.getLogger("audit");
    private final static Logger errorLogger = Logger.getLogger("errors");

    private final Socket connection;
    private final EventProcessor processor;

    EventListener(Socket connection, EventProcessor processor) {
        this.connection = connection;
        this.processor = processor;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = bufferedReaderFrom(connection);
            String payload;
            while ((payload = in.readLine()) != null) {
                auditLogger.info("Received event: " + payload);
                Event event = Event.fromPayload(payload);
                processor.submit(event);
            }
        } catch (IOException e) {
            errorLogger.warning("I/O error while receiving event: " + e.getMessage());
        }
    }
}
