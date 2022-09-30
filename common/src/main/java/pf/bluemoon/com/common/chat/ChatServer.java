package pf.bluemoon.com.common.chat;

import com.alibaba.fastjson.JSONObject;
import pf.bluemoon.com.common.nio.RpcModel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 14:27 2022/9/16
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ChatServer implements BaseProtocol {
    private Selector selector;
    private ServerSocketChannel listenerChannel;
    private SocketChannel responseChannel;
    private static final HashMap<String, Class> serviceRegistry = new HashMap<>();
    private String hostname;
    private int port;

    public ChatServer(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        init();
    }

    @Override
    public void init(){
        try {
            selector = Selector.open();
            // 初始化选择器
            listenerChannel = ServerSocketChannel.open();
            // 绑定主机地址和端口号
            listenerChannel.socket().bind(new InetSocketAddress(hostname, port));
            // 设置非阻塞模式
            listenerChannel.configureBlocking(false);
            // 将 listenerChannel 注册到 selector
            listenerChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void register(Class serviceInterface, Class impl) {
        serviceRegistry.put(serviceInterface.getName(), impl);
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

    @Override
    public byte[] listen(){
        try {
            while (true){
                int select = selector.select();
                if (select > 0){
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()){
                        SelectionKey key = iterator.next();
                        if (key.isAcceptable()){
                            SocketChannel accept = listenerChannel.accept();
                            accept.configureBlocking(false);
                            accept.register(selector, SelectionKey.OP_READ);
                            System.out.println(accept.getRemoteAddress() + " : 上线了");
                        }
                        if (key.isReadable()){
                            // 通道发送 read 事件，即通道是可读状态
                            receive(key);
                        }
                        iterator.remove();
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {

        }
        return new byte[0];
    }

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
//            int read = channel.read(buffer);
//            if (read > 0){
//                String result = new String(buffer.array());
//                System.out.println("接收到客户端请求内容：" + result);
//            }

            List<Byte> list = new ArrayList<>();
            while (channel.read(buffer) > 0){
                for (int i = 0; i < buffer.array().length; i++) {
                    list.add(buffer.array()[i]);
                }
            }
            bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                bytes[i] = list.get(i);
            }
            RpcModel rpcModel = JSONObject.parseObject(bytes, RpcModel.class);

            // 响应客户端
            responseChannel = channel;


            Class serviceClass = serviceRegistry.get(rpcModel.getServiceName());
            if (serviceClass == null) {
                throw new ClassNotFoundException(rpcModel.getServiceName() + " not found");
            }
            Object[] arguments = (Object[]) rpcModel.getArgs();
            Class<?>[] parameterTypes = (Class<?>[]) rpcModel.getParameterTypes();
            Method method = serviceClass.getMethod(rpcModel.getMethodName(), parameterTypes);
            Object result = method.invoke(serviceClass.newInstance(), arguments);

            // 如果方法有返回值，则相应回去
            if (!"void".equals(method.getReturnType().getName())){
                send(JSONObject.toJSONBytes(result));
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (null != key)
                    key.cancel();
                if (null != channel)
                    channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void send(byte[] bytes){
        try {
            responseChannel.write(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
