package pf.bluemoon.com.common.rpc;

/**
 * @Author chaoyou
 * @Date Create in 9:15 2022/9/13
 * @Modified by
 * @Version 1.0.0
 * @Description RPC的客户端调用通信协议方法，方法的执行在服务端；通信协议就是接口规范。
 */
public interface RPCProtocol {

    // 每个RPC协议都必须有一个版本号
    long versionID = 1023;

    void mkdir(String path);

    void put(String url, String path);

    String say(String url, String path);
}
