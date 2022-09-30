# 基本介绍
- Java BIO 就是传统的 java io 编程，其相关的类和接口在 java.io
- 同步并阻塞(传统阻塞型)，服务器实现模式为一个连接一个线程，即客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销
- BIO：适用于连接数目比较小且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK1.4 以前的唯一选择，但程序简单易理解。
# 工作机制
```
                              --------
                             | Server |
                              --------
                  ---------------|----------------
                 |               |                |
             --------         --------         --------
            | Thread |       | Thread |       | Thread |
             --------         --------         --------
               |                 |                  |
       ------------         ------------         ------------
      | Write/Read |       | Write/Read |       | Write/Read |
       ------------         ------------         ------------
             |                   |                    |
         --------             --------             --------
        | Client |           | Client |           | Client |
         --------             --------             --------
```
## 编程流程的梳理
- 服务器端启动一个 ServerSocket
- 客户端启动 Socket 对服务器进行通信，默认情况下服务器端需要为每个客户建立一个线程与之通讯
- 客户端发出请求后, 先咨询服务器是否有线程响应，如果没有则会等待，或者被拒绝
- 如果有响应，客户端线程会等待请求结束后，在继续执行
## 用例实战
- 使用 BIO 模型编写一个服务器端，监听 6666 端口，当有客户端连接时，就启动一个线程与之通讯。
- 要求使用线程池机制改善，可以连接多个客户端.
- 服务器端可以接收客户端发送的数据(telnet 方式即可)。
### 服务端
```java
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
```
### 客户端
```java
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

```
## 问题分析
- 每个请求都需要创建独立的线程，与对应的客户端进行数据 Read/Write 的业务处理。
- 当并发数较大时，需要创建大量线程来处理连接，系统资源占用较大。
- 连接建立后，如果当前线程暂时没有数据可读，则线程就阻塞在 Read 操作上，造成线程资源浪费
## 优化方向
这里实现的简单RPC框架是使用Java语言开发，与Java语言高度耦合，并且通信方式采用的Socket是基于BIO实现的，IO效率不高，还有Java原生的序列化机制占内存太多，运行效率也不高。可以考虑从下面几种方法改进。
- 可以采用基于JSON数据传输的RPC框架；
- 可以使用NIO或直接使用Netty替代BIO实现；
- 使用开源的序列化机制，如Hadoop Avro与Google protobuf等；
- 服务注册可以使用Zookeeper进行管理，能够让应用更加稳定。

