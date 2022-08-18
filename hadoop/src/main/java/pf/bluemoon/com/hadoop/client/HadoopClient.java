package pf.bluemoon.com.hadoop.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @Author chaoyou
 * @Date Create in 11:17 2022/8/8
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class HadoopClient {
    private static final Logger logger = LoggerFactory.getLogger(HadoopClient.class);
    private static FileSystem fs = null;
    private static String hdfsDriver = "hdfs";
    private static String host = "localhost";
    private static Integer port = 8020;
    private static String username = "chaoyou";

    public static FileSystem initClient() {
        try {
            logger.info("================ 开始执行 hadoop 客户端初始化操作 =================");
            // 1、获取文件系统
            Configuration config = new Configuration();
            if (null == fs){
                synchronized (FileSystem.class){
                    if (null == fs){
                        String driver = getDriver(hdfsDriver);
                        if (null != driver && driver.length() > 0){
                            fs = FileSystem.get(new URI(driver), config, username);
                        }
                    }
                }
            }
            logger.info("================ 完成执行 hadoop 客户端初始化操作 =================");
        } catch (Exception e){
            logger.error("hadoop 客户端初始化操作失败：{}", e.getMessage());
            e.printStackTrace();
        }
        return fs;
    }

    private static String getDriver(String hdfsDriver){
        StringBuffer buffer = new StringBuffer();
        buffer.append(hdfsDriver).append("://").append(host).append(":").append(port);
        return buffer.toString();
    }

    public static void close(){
        try {
            logger.info("================ 开始执行 hadoop 客户端关闭操作 =================");
            if (null != fs){
                fs.close();
            }
            logger.info("================ 完成执行 hadoop 客户端关闭操作 =================");
        } catch (Exception e){
            logger.error("hadoop 客户端关闭操作失败：{}", e.getMessage());
            e.printStackTrace();
        }
    }
}
