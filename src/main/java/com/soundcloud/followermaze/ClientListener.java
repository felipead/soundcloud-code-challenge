package com.soundcloud.followermaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;

class ClientListener implements Runnable {

    private final Socket connection;
    private final EventRouter eventRouter;

    ClientListener(Socket connection, EventRouter eventRouter) {
        this.connection = connection;
        this.eventRouter = eventRouter;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = bufferedReaderFrom(connection);
            String line = in.readLine();
            if (line == null) {
                // Socket disconnected
                return;
            }
            registerClient(Long.parseLong(line));
        } catch (IOException ignore) {
            // TODO: log this occurrence (error-level)
        }
    }

    private void registerClient(Long id) {
        // TODO: write informative logs instead
        System.out.println(String.format("Client %d accepted.", id));
        eventRouter.register(new Client(id, connection));
    }
}
