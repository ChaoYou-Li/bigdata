package pf.bluemoon.com.hadoop.rpc;

import pf.bluemoon.com.common.chat.ChatClient;
import pf.bluemoon.com.common.nio.NioClient;
import pf.bluemoon.com.common.nio.RpcModel;
import pf.bluemoon.com.common.rpc.RPCClient;
import pf.bluemoon.com.common.rpc.RPCProtocol;

import java.net.InetSocketAddress;

/**
 * @Author chaoyou
 * @Date Create in 9:19 2022/9/13
 * @Modified by
 * @Version 1.0.0
 * @Description 这是一个模拟HDFS客户端向NameNode发起操作Request
 */
public class HDFSClient {

    public static void main(String[] args) {
        NioClient client = new NioClient("127.0.0.1", 8888);
        RPCProtocol protocol = client.getRemoteProxyObj(RPCProtocol.class);
        System.out.println(protocol.say("liubei.txt", "/sanguo"));
        protocol.put("liubei.txt", "/sanguo");
        protocol.mkdir("/sanguo");
    }

}
