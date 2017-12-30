package com.soundcloud.followermaze;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.soundcloud.followermaze.SocketUtils.bufferedWriterFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientListenerTest extends AbstractSocketServerTest {

    private EventRouter eventRouter;

    @Before
    public void setup() {
        eventRouter = mock(EventRouter.class);
    }

    @Test
    public void registersClientSuccessfully() throws Exception {
        final Long id = 12345L;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new ClientListener(clientConnection, eventRouter));

        Writer out = bufferedWriterFrom(serverConnection);
        out.write(id.toString());
        out.write("\r\n");
        out.flush();

        promise.get();

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(eventRouter).register(clientCaptor.capture());
        Client client = clientCaptor.getValue();
        assertThat(client.getId(), is(id));
    }

    @Test
    public void clientDisconnectsPrematurely() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future promise = executor.submit(new ClientListener(clientConnection, eventRouter));

        serverConnection.close();
        promise.get();

        verify(eventRouter, never()).register(any(Client.class));
    }
}
