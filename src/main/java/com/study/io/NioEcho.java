package com.study.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioEcho {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(8080));
        server.configureBlocking(false);                                 // ① 必须先设成非阻塞
        server.register(selector, SelectionKey.OP_ACCEPT);               // ② 登记：我关心"有新连接"

        while (true) {
            selector.select();                                           // ③ 整个程序唯一会停的地方

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();                                             // ④ 必须手动删，否则下次还在

                if (key.isAcceptable()) {                                // 有客户端连进来了
                    SocketChannel ch = ((ServerSocketChannel) key.channel()).accept();
                    ch.configureBlocking(false);
                    ch.register(selector, SelectionKey.OP_READ);         // 把新连接也挂上去
                } else if (key.isReadable()) {                           // 某条连接有数据了
                    SocketChannel ch = (SocketChannel) key.channel();
                    ByteBuffer buf = ByteBuffer.allocate(1024);
                    int n = ch.read(buf);                                // ⑤ 这一行永远不会停
                    if (n == -1) { ch.close(); continue; }
                    buf.flip();
                    ch.write(buf);
                }
            }
        }
    }
}
