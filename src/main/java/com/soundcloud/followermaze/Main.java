package com.soundcloud.followermaze;

import java.io.IOException;

class Main {
    public static void main(String[] args) throws IOException {
        Server server = new Server(9090, 9099);
        server.run();
    }
}
