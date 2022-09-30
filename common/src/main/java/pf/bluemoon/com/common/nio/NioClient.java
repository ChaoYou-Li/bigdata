package pf.bluemoon.com.common.nio;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @Author chaoyou
 * @Date Create in 17:57 2022/9/15
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class NioClient {
    private static final Logger logger = LoggerFactory.getLogger(NioClient.class);

    private int port;
    private volatile boolean isRunning = true;
    private static boolean configBlock;
    private String hostname;
    private static Selector selector;
    private static SocketChannel socketChannel;

    public NioClient(int port) {
        this("127.0.0.1", port, false);
    }

    public NioClient(int port, boolean configBlock) {
        this("127.0.0.1", port, configBlock);
    }

    public NioClient(String hostname, int port) {
        this(hostname, port, false);
    }

    public NioClient(String hostname, int port, boolean configBlock) {
        this.hostname = hostname;
        this.port = port;
        this.configBlock = configBlock;
        try {
            this.selector = Selector.open();
//            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public <T> T getRemoteProxyObj(final Class<?> serviceInterface) {
        /**
         * 1.将本地的接口调用转换成JDK的动态代理，在动态代理中实现接口的远程调用
         */
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[]{serviceInterface},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            /**
                             *  2.创建Socket客户端，根据指定地址连接远程服务提供者
                             */
                            doConnect();

                            /**
                             * 3.将远程服务调用所需的接口类、方法名、参数列表等编码后发送给服务提供者
                             */
                            RpcModel rpcModel = new RpcModel();
                            rpcModel.setServiceName(serviceInterface.getName());
                            rpcModel.setMethodName(method.getName());
                            rpcModel.setParameterTypes(method.getParameterTypes());
                            rpcModel.setArgs(args);
                            // 序列化：对象 -> 字节
                            byte[] writeBytes = JSONObject.toJSONBytes(rpcModel);
                            // 创建一个写缓冲区
                            ByteBuffer writeBuffer = ByteBuffer.allocate(writeBytes.length);
                            // 把要响应的数据字节数组放入写缓冲区
                            writeBuffer.put(writeBytes);
                            // 把缓冲区转为可写状态
                            writeBuffer.flip();
                            // 缓冲区数据写入通道进行传输
                            socketChannel.write(writeBuffer);

                            /**
                             * 4.同步阻塞等待服务器返回应答，获取应答后返回
                             */
                            byte[] receive = null;
                            try {
                                selector.select(1000);
                                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                                while (iterator.hasNext()){
                                    SelectionKey key = iterator.next();
                                    iterator.remove();
                                    try {
                                        receive = handleInput(key);
                                    } catch (Exception e){
                                        if (null != key)
                                            key.cancel();
                                        if (null != key.channel()){
                                            key.channel().close();
                                        }
                                    }
                                }
                            } catch (Exception e){
                                e.printStackTrace();
                            }
                            if ("void".equals(method.getReturnType().getName())){
                                return null;
                            }
                            return JSONObject.parseObject(receive, method.getReturnType());
                        } finally {
                            close();
                        }
                    }
                });
    }

    private void doConnect(){
        try {
            // 绑定主机地址和端口号
            socketChannel = SocketChannel.open(new InetSocketAddress(hostname, port));
            // 设置非阻塞模式
            socketChannel.configureBlocking(configBlock);
            // 将 socketChannel 注册到 selector
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void close(){
        try {
            if (null != socketChannel)
                socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] handleInput(SelectionKey key) throws IOException {
        byte[] receive = null;
        if (key.isValid()){
            // 取到关联的channel，获取通道
            SocketChannel channel = (SocketChannel) key.channel();
            if (key.isConnectable()){
                if (channel.finishConnect()){
                    channel.register(selector, SelectionKey.OP_READ);
                } else {
                    // 连接失败，直接退出程序
                    System.exit(1);
                }
            }
            if (key.isReadable()){
                // 创建缓冲区
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);

                // 把管道中的数据读入缓冲区
                int readBytes = channel.read(readBuffer);
                if (readBytes > 0) {
                    // 将缓冲区的limit设置为position（position = 0）
                    readBuffer.flip();
                    // 根据缓冲区实际可读大小创建字节数组容量
                    receive = new byte[readBuffer.remaining()];
                    // 把缓冲区中的字节数据复制到数组中
                    readBuffer.get(receive);
                    isRunning = false;
                } else if (readBytes < 0) {
                    // 关闭客户端通道
                    key.cancel();
                    channel.close();
                } else {
                    ;   // 传递的数据为 0 个字节
                }
            }
        }
        return receive;
    }

}
