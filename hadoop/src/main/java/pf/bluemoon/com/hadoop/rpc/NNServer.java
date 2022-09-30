package pf.bluemoon.com.hadoop.rpc;

import pf.bluemoon.com.common.chat.ChatServer;
import pf.bluemoon.com.common.nio.NioServer;
import pf.bluemoon.com.common.rpc.RPCProtocol;
import pf.bluemoon.com.common.rpc.ServiceCenter;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 9:17 2022/9/13
 * @Modified by
 * @Version 1.0.0
 * @Description 这是一个模拟 NameNode，来处理HDFS发过来的Request
 */
public class NNServer implements RPCProtocol {

    @Override
    public void mkdir(String path) {
        System.out.println("创建目录：" + path);
    }

    @Override
    public void put(String url, String path) {
        System.out.println("上传文件：" + url + "，到 hdfs 的：" + path + "目录");
    }

    @Override
    public String say(String url, String path) {
        return "上传文件：" + url + "，到 hdfs 的：" + path + "目录";
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        NioServer center = new NioServer("127.0.0.1", 8888);
        center.register(RPCProtocol.class, NNServer.class);
        System.out.println("================================ 服务器开始工作 ==================================");
        center.start();
    }

}
