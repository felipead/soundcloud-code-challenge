package com.soundcloud.followermaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;

class EventListener implements Runnable {

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
                // TODO: log that event was received (info-level at audit log)
                System.out.println(payload);
                Event event = Event.fromPayload(payload);
                processor.submit(event);
            }
        } catch (IOException ignored) {
            // TODO: log this occurrence (error-level)
        }
    }
}
