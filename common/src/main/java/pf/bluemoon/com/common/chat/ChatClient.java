package pf.bluemoon.com.common.chat;

import com.alibaba.fastjson.JSONObject;
import pf.bluemoon.com.common.nio.RpcModel;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 14:27 2022/9/16
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ChatClient implements BaseProtocol {

    private Selector selector;
    private SocketChannel sendChannel;
    private String hostname;
    private int port;

    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        init();
    }

    @Override
    public void init(){
        try {
            selector = Selector.open();
            // 初始化选择器
            sendChannel = SocketChannel.open(new InetSocketAddress(hostname, port));
            // 绑定主机地址和端口号
            // 设置非阻塞模式
            sendChannel.configureBlocking(false);
            // 将 listenerChannel 注册到 selector
            sendChannel.register(selector, SelectionKey.OP_READ);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (null != selector){
                selector.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 向服务器端发送数据
     *
     * @param bytes
     */
    @Override
    public void send(byte[] bytes) {
        try {
//            // 初始化选择器
//            sendChannel = SocketChannel.open(new InetSocketAddress(hostname, port));
//            // 绑定主机地址和端口号
//            // 设置非阻塞模式
//            sendChannel.configureBlocking(false);
//            // 将 listenerChannel 注册到 selector
//            sendChannel.register(selector, SelectionKey.OP_READ);
            sendChannel.write(ByteBuffer.wrap(bytes));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 监听每次请求的响应
     */
    @Override
    public byte[] listen(){
        try {
            int select = selector.select(1000);
            if (select > 0){
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if (key.isReadable()){
                        // 通道发送 read 事件，即通道是可读状态
                        return receive(key);
                    }
                    iterator.remove();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {

        }
        return new byte[0];
    }

    /**
     *
     * @param key
     */
    @Override
    public byte[] receive(SelectionKey key){
        // 取到关联的channel
        SocketChannel channel = null;
        byte[] bytes = null;
        try {
            // 获取通道
            channel = (SocketChannel) key.channel();

            // 创建缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            // 把管道中的数据读入缓冲区
            List<Byte> list = new ArrayList<>();
            while (channel.read(buffer) > 0){
                byte[] array = buffer.array();
                for (int i = 0; i < array.length; i++) {
                    list.add(array[i]);
                }
            }
            bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                bytes[i] = list.get(i);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
//            try {
//                if (null != key)
//                    key.cancel();
//                if (null != channel)
//                    channel.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        return bytes;
    }

    public <T> T getRemoteProxyObj(final Class<?> serviceInterface) {
        // 1.将本地的接口调用转换成JDK的动态代理，在动态代理中实现接口的远程调用
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[]{serviceInterface},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        try {
                            // 2.创建Socket客户端，根据指定地址连接远程服务提供者

                            // 3.将远程服务调用所需的接口类、方法名、参数列表等编码后发送给服务提供者
                            RpcModel rpcModel = new RpcModel();
                            rpcModel.setServiceName(serviceInterface.getName());
                            rpcModel.setMethodName(method.getName());
                            rpcModel.setParameterTypes(method.getParameterTypes());
                            rpcModel.setArgs(args);
                            send(JSONObject.toJSONBytes(rpcModel));

                            // 4.同步阻塞等待服务器返回应答，获取应答后返回
                            byte[] receive = null;
                            int select = selector.select(1000);
                            if (select > 0){
                                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                                while (iterator.hasNext()){
                                    SelectionKey key = iterator.next();
                                    if (key.isReadable()){
                                        // 通道发送 read 事件，即通道是可读状态
                                        receive = receive(key);
                                    }
                                }
                                iterator.remove();
                            }
                            if ("void".equals(method.getReturnType().getName())){
                                return null;
                            }
                            return JSONObject.parseObject(receive, method.getReturnType());
                        } finally {
//                            if (null != sendChannel){
//                                sendChannel.close();
//                            }
                        }
                    }
                });
    }
}
