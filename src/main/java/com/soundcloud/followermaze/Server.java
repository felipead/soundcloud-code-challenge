package com.soundcloud.followermaze;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Server {

    private final static int CLIENT_LISTENER_THREADS = 100;

    private final static Logger auditLogger = Logger.getLogger("audit");

    private final int clientPort;
    private final int eventSourcePort;

    private final EventRouter eventRouter = new EventRouter();
    private final EventProcessor eventProcessor = new EventProcessor(eventRouter);

    private ExecutorService eventProcessorWorker = Executors.newSingleThreadExecutor();
    private ExecutorService eventListenerWorker = Executors.newSingleThreadExecutor();
    private ExecutorService clientListenerWorker = Executors.newFixedThreadPool(CLIENT_LISTENER_THREADS);

    Server(int eventSourcePort, int clientPort) {
        this.eventSourcePort = eventSourcePort;
        this.clientPort = clientPort;
    }

    public void run() throws IOException {
        newEventProcessorThread();
        acceptEventSourceConnection();
        acceptClientConnections();
    }

    private void newEventProcessorThread() {
        eventProcessorWorker.submit(eventProcessor);
    }

    private void acceptEventSourceConnection() throws IOException {
        auditLogger.info("Listening event source connection on port " + eventSourcePort);
        try (ServerSocket server = new ServerSocket(eventSourcePort)) {
            Socket connection = server.accept();
            eventListenerWorker.submit(new EventListener(connection, eventProcessor));
        }
    }

    private void acceptClientConnections() throws IOException {
        auditLogger.info("Listening client connections on port " + clientPort);
        try (ServerSocket server = new ServerSocket(this.clientPort)) {
            while (true) {
                Socket connection = server.accept();
                clientListenerWorker.submit(new ClientListener(connection, eventRouter));
            }
        }
    }
}
