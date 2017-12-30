package com.soundcloud.followermaze;

import java.io.IOException;
import java.io.Writer;
import java.net.Socket;

import static com.soundcloud.followermaze.SocketUtils.bufferedWriterFrom;

public class Client {

    private final Long id;
    private final Socket connection;

    Client(Long id, Socket connection) {
        this.id = id;
        this.connection = connection;
    }

    public void send(Event event) throws IOException {
        Writer out = bufferedWriterFrom(this.connection);
        out.write(event.toPayload());
        out.write("\r\n");
        out.flush();
    }

    public Long getId() {
        return id;
    }
}
