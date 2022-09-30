package pf.bluemoon.com.hadoop;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pf.bluemoon.com.common.nio.RpcModel;
import pf.bluemoon.com.common.rpc.RPCProtocol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @Author chaoyou
 * @Date Create in 11:48 2022/9/19
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@SpringBootTest
public class RpcTest {
    @Test
    void test01(){
        RPCProtocol protocol = getRemoteProxyObj(RPCProtocol.class);
        protocol.put("liubei.txt", "/sanguo");
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
                            byte[] bytes = JSONObject.toJSONBytes(rpcModel);
                            System.out.println(Arrays.toString(bytes));
                            rpcModel = JSONObject.parseObject(bytes, RpcModel.class);
                            System.out.println(rpcModel);
                            return null;
                        } finally {
                        }
                    }
                });
    }
}
