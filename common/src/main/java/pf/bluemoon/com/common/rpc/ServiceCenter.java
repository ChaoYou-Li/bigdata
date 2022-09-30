package pf.bluemoon.com.common.rpc;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author chaoyou
 * @Date Create in 17:46 2022/9/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Data
public class ServiceCenter implements Server {
    private static final Logger logger = LoggerFactory.getLogger(ServiceCenter.class);

    private static final HashMap<String, Class> serviceRegistry = new HashMap<>();
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static boolean isRunning = false;
    private static int port;
    private static String host;

    public ServiceCenter(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void stop() {
        isRunning = false;
        executor.shutdown();
    }

    @Override
    public void start() throws IOException {
        ServerSocket server = new ServerSocket();
        server.bind(new InetSocketAddress(host, port));
        logger.info("===================== start rpc server ====================");
        try {
            while (true) {
                // 1.//2、调用accept()方法开始监听，等待客户端的连接,监听客户端的TCP连接，接到TCP连接后将其封装成task，由线程池执行
                Socket socket = server.accept();
                executor.execute(new ServiceTask(socket));
            }
        } finally {
            server.close();
            logger.info("===================== end rpc server ====================");
        }
    }

    @Override
    public void register(Class serviceInterface, Class impl) {
        serviceRegistry.put(serviceInterface.getName(), impl);
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPort() {
        return port;
    }

    private static class ServiceTask implements Runnable {
        Socket clent;

        public ServiceTask(Socket client) {
            this.clent = client;
        }

        public void run() {
            ObjectInputStream input = null;
            ObjectOutputStream output = null;
            try {
                // 2.将客户端发送的码流反序列化成对象，反射调用服务实现者，获取执行结果
                input = new ObjectInputStream(clent.getInputStream());
                String serviceName = input.readUTF();
                String methodName = input.readUTF();
                Class<?>[] parameterTypes = (Class<?>[]) input.readObject();
                Object[] arguments = (Object[]) input.readObject();
                Class serviceClass = serviceRegistry.get(serviceName);
                if (serviceClass == null) {
                    throw new ClassNotFoundException(serviceName + " not found");
                }
                Method method = serviceClass.getMethod(methodName, parameterTypes);
                Object result = method.invoke(serviceClass.newInstance(), arguments);

                // 3.将执行结果反序列化，通过socket发送给客户端
                output = new ObjectOutputStream(clent.getOutputStream());
                output.writeObject(result);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (clent != null) {
                    try {
                        clent.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}
