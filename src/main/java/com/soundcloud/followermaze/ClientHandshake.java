package com.soundcloud.followermaze;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import static com.soundcloud.followermaze.SocketUtils.bufferedReaderFrom;

/**
 * This class is responsible for handshaking clients. It receives
 * a TCP connection that was just established with a client, performs
 * the protocol handshake and registers the client with an instance of
 * {@link EventRouter}.
 *
 * The handshake is as follows: the client connects, sends its
 * numeric ID as plain-text, followed by CRLF.
 */
class ClientHandshake implements Runnable {

    private final static Logger auditLogger = Logger.getLogger("audit");
    private final static Logger errorLogger = Logger.getLogger("error");

    private final Socket connection;
    private final EventRouter eventRouter;

    ClientHandshake(Socket connection, EventRouter eventRouter) {
        this.connection = connection;
        this.eventRouter = eventRouter;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = bufferedReaderFrom(connection);
            String line = in.readLine();
            if (line == null) {
                auditLogger.info("Client disconnected prematurely before sending its id");
                return;
            }
            registerClient(Long.parseLong(line));
        } catch (IOException e) {
            errorLogger.warning("I/O error while registering client: " + e.getMessage());
        }
    }

    private void registerClient(Long id) {
        auditLogger.info("Registered client with id: " + id);
        eventRouter.register(new Client(id, connection));
    }
}
