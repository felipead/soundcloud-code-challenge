package com.soundcloud.followermaze;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.soundcloud.followermaze.SocketUtils.silentClose;

public abstract class AbstractSocketServerTest {

    private static ServerSocket testServer;
    Socket serverConnection;
    Socket clientConnection;

    @BeforeClass
    public static void baseSetupClass() throws IOException {
        testServer = new ServerSocket(0);
    }

    @Before
    public void baseSetup() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Socket> promise = executor.submit(() -> testServer.accept());
        clientConnection = new Socket("localhost", testServer.getLocalPort());
        serverConnection = promise.get();
    }

    @After
    public void baseTeardown() {
        silentClose(clientConnection);
        silentClose(serverConnection);
    }

    @AfterClass
    public static void baseTeardownClass() {
        silentClose(testServer);
    }
}
