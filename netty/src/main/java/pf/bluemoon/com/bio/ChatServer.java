package pf.bluemoon.com.bio;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author chaoyou
 * @Date Create in 9:02 2022/9/22
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ChatServer {
    private static final int port = 6666;
    public static void response() throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("====================== 服务器开始运行 ====================");
        while (true){
            System.out.println(" 线程信息id：" + Thread.currentThread().getId() + " | 名字：" +
                    Thread.currentThread().getName());
            //监听，等待客户端连接
            System.out.println("等待连接....");
            final Socket socket = serverSocket.accept();
            System.out.println("连接到一个客户端");
            //就创建一个线程，与之通讯(单独写一个方法)
            executorService.execute(new Runnable() {
                public void run() { //我们重写
                    //可以和客户端通讯
                    handler(socket);
                }
            });
        }
    }
    private static void handler(Socket socket) {
        InputStream inputStream = null;
        try {
            System.out.println("线程信息id：" + Thread.currentThread().getId() + " | 名字：" +
                    Thread.currentThread().getName());
            byte[] bytes = new byte[1024];
            //通过 socket 获取输入流
            inputStream = socket.getInputStream();
            //循环的读取客户端发送的数据
            while (true) {
                System.out.println("线程信息id：" + Thread.currentThread().getId() + " | 名字：" +
                        Thread.currentThread().getName());
                System.out.println("read....");
                int read = inputStream.read(bytes);
                if(read != -1) {
                    System.out.println(new String(bytes, 0, read)); //输出客户端发送的数据
                } else {
                    break;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("关闭和 client 的连接");
            try {
                if (null != socket){
                    socket.close();
                }
                if (null != inputStream){
                    inputStream.close();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
