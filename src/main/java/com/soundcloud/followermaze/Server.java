package com.soundcloud.followermaze;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {

    private final static int CLIENT_HANDSHAKE_THREADS = 100;

    private final static Logger auditLogger = Logger.getLogger("audit");

    private final int clientPort;
    private final int eventSourcePort;

    private final EventRouter eventRouter = new EventRouter();
    private final EventDispatcher eventDispatcher = new EventDispatcher(eventRouter);

    private ExecutorService eventDispatcherWorker = Executors.newSingleThreadExecutor();
    private ExecutorService eventReceiverWorker = Executors.newSingleThreadExecutor();
    private ExecutorService clientHandshakeWorker = Executors.newFixedThreadPool(CLIENT_HANDSHAKE_THREADS);

    Server(int eventSourcePort, int clientPort) {
        this.eventSourcePort = eventSourcePort;
        this.clientPort = clientPort;
    }

    public void run() throws IOException {
        newEventDispatcherThread();
        acceptEventSourceConnection();
        acceptClientConnections();
    }

    private void newEventDispatcherThread() {
        eventDispatcherWorker.submit(eventDispatcher);
    }

    private void acceptEventSourceConnection() throws IOException {
        auditLogger.info("Listening event source connection on port " + eventSourcePort);
        try (ServerSocket server = new ServerSocket(eventSourcePort)) {
            Socket connection = server.accept();
            eventReceiverWorker.submit(new EventReceiver(connection, eventDispatcher));
        }
    }

    private void acceptClientConnections() throws IOException {
        auditLogger.info("Listening client connections on port " + clientPort);
        try (ServerSocket server = new ServerSocket(this.clientPort)) {
            while (true) {
                Socket connection = server.accept();
                clientHandshakeWorker.submit(new ClientHandshake(connection, eventRouter));
            }
        }
    }
}
