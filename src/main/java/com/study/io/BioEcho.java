package com.study.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BioEcho {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8080);
        while (true) {
            Socket socket = server.accept();              // ① 停在这里，直到有客户端连进来
            new Thread(() -> handle(socket)).start();     // ② 所以必须开一个新线程
        }
    }

    static void handle(Socket socket) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) != -1) {            // ③ 停在这里，直到数据到达
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}