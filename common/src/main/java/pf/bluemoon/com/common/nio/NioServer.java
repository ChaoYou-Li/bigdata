package pf.bluemoon.com.common.nio;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pf.bluemoon.com.common.rpc.Server;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * @Author chaoyou
 * @Date Create in 16:19 2022/9/15
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class NioServer implements Server {
    private static final Logger logger = LoggerFactory.getLogger(NioServer.class);

    private int port;
    private volatile boolean isRunning = true;
    private static boolean configBlock;
    private static final HashMap<String, Class> serviceRegistry = new HashMap<>();
    private String hostname;
    private static Selector selector;
    private static ServerSocketChannel listenerChannel;

    public NioServer(int port) {
        this("127.0.0.1", port, false);
    }

    public NioServer(int port, boolean configBlock) {
        this("127.0.0.1", port, configBlock);
    }

    public NioServer(String hostname, int port) {
        this(hostname, port, false);
    }

    public NioServer(String hostname, int port, boolean configBlock) {
        this.hostname = hostname;
        this.port = port;
        this.configBlock = configBlock;
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public void start() throws IOException {
        // 初始化选择器
        listenerChannel = ServerSocketChannel.open();
        // 绑定主机地址和端口号
        listenerChannel.socket().bind(new InetSocketAddress(hostname, port), 1024);
        // 设置非阻塞模式
        listenerChannel.configureBlocking(configBlock);
        // 将 listenerChannel 注册到 selector
        listenerChannel.register(selector, SelectionKey.OP_ACCEPT);

        logger.info("===================== start rpc server ====================");
        try {
            // 1.//2、调用accept()方法开始监听，等待客户端的连接,监听客户端的TCP连接，接到TCP连接后将其封装成task，由线程池执行
            while (isRunning){
                // 阻塞1000ms，轮询复用选择器中的 selectionKey
                int select = selector.select(1000);
                if (select > 0){
                    // 存在 selectionKey
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        // 处理 selectionKey
                        handleInput(iterator.next());
                        // 去除已处理的 selectionKey，避免重复处理
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e){
          e.printStackTrace();
        } finally {
//            if (null != listenerChannel){
//                listenerChannel.close();
//            }
//            if (null != selector){
//                selector.close();
//            }
            logger.info("===================== end rpc server ====================");
        }
    }

    @Override
    public void register(Class serviceInterface, Class impl) {
        serviceRegistry.put(serviceInterface.getName(), impl);
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public int getPort() {
        return 0;
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()){
            /**
             * 处理新接入的客户端请求
             */
            if (key.isAcceptable()){
                // 根据 SelectionKey 的操作位进行判断可知网络事件的类型
                ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                // 接收客户端连接，并创建连接通道
                SocketChannel accept = socketChannel.accept();
                // 设置通道为非阻塞模式
                accept.configureBlocking(false);
                // 把通道注册到多路复用选择器中
                accept.register(selector, SelectionKey.OP_READ);
                logger.info(accept.getRemoteAddress() + " : 上线了");
            }

            /**
             * 读取客户端的请求消息
             */
            if (key.isReadable()){
                // 取到关联的channel
                SocketChannel channel = null;
                byte[] bytes = null;
                try {
                    // 获取通道
                    channel = (SocketChannel) key.channel();

                    // 创建缓冲区
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);

                    // 把管道中的数据读入缓冲区
                    int readBytes = channel.read(readBuffer);
                    if (readBytes > 0){
                        // 将缓冲区的limit设置为position（position = 0）
                        readBuffer.flip();
                        // 根据缓冲区实际可读大小创建字节数组容量
                        bytes = new byte[readBuffer.remaining()];
                        // 把缓冲区中的字节数据复制到数组中
                        readBuffer.get(bytes);
                    } else if (readBytes < 0){
                        // 关闭客户端通道
                        key.cancel();
                        channel.close();
                    } else {
                        ;   // 传递的数据为 0 个字节
                    }
                    // 反序列化：字节 -> 对象
                    RpcModel rpcModel = JSONObject.parseObject(bytes, RpcModel.class);
                    if (null == rpcModel){
                        return;
                    }
                    // 消费者调用方法实参列表
                    Object[] arguments = (Object[]) rpcModel.getArgs();
                    // 消费者调用方法形参列表
                    Class<?>[] parameterTypes = (Class<?>[]) rpcModel.getParameterTypes();
                    // 从注册中心中取出对应服务对象
                    Class serviceClass = serviceRegistry.get(rpcModel.getServiceName());
                    if (serviceClass == null) {
                        logger.error(rpcModel.getServiceName() + " not found");
                    }
                    // 利用反射构造生产者的服务方法
                    Method method = serviceClass.getMethod(rpcModel.getMethodName(), parameterTypes);
                    // 利用反射调用生产者的服务方法
                    logger.info("开始调用服务：" + method.getName());
                    Object result = method.invoke(serviceClass.newInstance(), arguments);
                    logger.info("结束调用服务：" + method.getName());

                    // 如果方法有返回值，则相应回去（异步操作）
                    if (!"void".equals(method.getReturnType().getName())){
                        // 序列化：对象 -> 字节
                        byte[] writeBytes = JSONObject.toJSONBytes(result);
                        // 创建一个写缓冲区
                        ByteBuffer writeBuffer = ByteBuffer.allocate(writeBytes.length);
                        // 把要响应的数据字节数组放入写缓冲区
                        writeBuffer.put(writeBytes);
                        // 把缓冲区转为可写状态
                        writeBuffer.flip();
                        // 缓冲区数据写入通道进行传输
                        channel.write(writeBuffer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } finally {
                    if (null != channel){
                        logger.info(channel.getRemoteAddress() + " : 下线了");
                        channel.close();
                    }
                }
            }
        }
    }
}
