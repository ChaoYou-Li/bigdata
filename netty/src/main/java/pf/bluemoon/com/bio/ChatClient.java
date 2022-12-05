package pf.bluemoon.com.bio;

import jdk.nashorn.internal.ir.CallNode;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author chaoyou
 * @Date Create in 9:01 2022/9/22
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ChatClient {
    private static final int port = 6666;
    public static void request(String msg) {
        Socket socket = null;
        OutputStream outputStream = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(port));
            outputStream = socket.getOutputStream();
            outputStream.write(msg.getBytes());
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (null != outputStream){
                    outputStream.close();
                }
                if (null != socket){
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
