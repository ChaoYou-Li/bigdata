package pf.bluemoon.com.hadoop.tool;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.util.Arrays;

/**
 * @Author chaoyou
 * @Date Create in 18:10 2022/8/21
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class WordCountDriver {

    private static Tool tool;

    public static void drive(String[] args) throws Exception {
        // 1. 创建配置文件
        Configuration conf = new Configuration();

        // 2. 判断是否有tool接口
        switch (args[0]){
            case "wordcount":
                tool = new WordCountTool();
                break;
            default:
                throw new RuntimeException(" No such tool: "+ args[0] );
        }
        // 3. 用Tool执行程序
        // Arrays.copyOfRange 将老数组的元素放到新数组里面
        // 最后两个参数必须是input和output
        int run = ToolRunner.run(conf, tool, Arrays.copyOfRange(args, args.length - 2, args.length));

        System.exit(run);
    }

}
