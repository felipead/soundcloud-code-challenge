package com.soundcloud.followermaze;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

final class SocketUtils {

    static BufferedReader bufferedReaderFrom(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    static BufferedWriter bufferedWriterFrom(Socket socket) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    static void silentClose(Socket socket) {
        try { socket.close(); } catch (IOException ignored) { }
    }

    static void silentClose(ServerSocket socket) {
        try { socket.close(); } catch (IOException ignored) { }
    }
}
