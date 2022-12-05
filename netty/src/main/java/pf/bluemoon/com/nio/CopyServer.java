package pf.bluemoon.com.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @Author chaoyou
 * @Date Create in 23:29 2022/9/22
 * @Modified by
 * @Version 1.0.0
 * @Description NIO—零拷贝：文件接收方
 */
public class CopyServer {
    public static void response() throws IOException {
        InetSocketAddress address = new InetSocketAddress(7001);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = serverSocketChannel.socket();
        serverSocket.bind(address);
        // 创建 buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            int readcount = 0;
            while (-1 != readcount) {
                try {
                    readcount = socketChannel.read(byteBuffer);
                }catch (Exception ex) {
                    break;
                }
                // 倒带 position = 0 mark 作废
                byteBuffer.rewind();
            }
        }
    }
}
