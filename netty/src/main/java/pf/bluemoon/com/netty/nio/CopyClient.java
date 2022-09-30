package pf.bluemoon.com.netty.nio;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @Author chaoyou
 * @Date Create in 23:29 2022/9/22
 * @Modified by
 * @Version 1.0.0
 * @Description NIO—零拷贝：文件发送方
 */
public class CopyClient {
    public static void request(String path) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 7001));
        //得到一个文件 channel
        FileChannel fileChannel = new FileInputStream(path).getChannel();
        //准备发送
        long startTime = System.currentTimeMillis();
        //在 linux 下一个 transferTo 方法就可以完成传输
        //在 windows 下 一次调用 transferTo 只能发送 8m , 就需要分段传输文件, 而且要主要
        //传输时的位置 =》 课后思考...
        //transferTo 底层使用到零拷贝
        long transferCount = fileChannel.transferTo(0, fileChannel.size(), socketChannel);
        System.out.println("发送的总的字节数：" + transferCount + "，耗时：" + (System.currentTimeMillis() - startTime));
        //关闭
        fileChannel.close();
    }
}
