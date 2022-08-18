package pf.bluemoon.com.hadoop.mapjoin;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import pf.bluemoon.com.hadoop.reducejoin.TableBean;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Author: chaoyou
 * Email：1277618785@qq.com
 * CSDN：https://blog.csdn.net/qq_41910568
 * Date: 22:11 2022/8/14
 * Content：
 */
public class JoinDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException, URISyntaxException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(JoinDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(JoinMapper.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(TableBean.class);
        job.setMapOutputValueClass(NullWritable.class);

        // 设置最后输出类型
        job.setOutputKeyClass(TableBean.class);
        job.setOutputValueClass(NullWritable.class);

        // 设置取消reduce阶段
        job.setNumReduceTasks(0);

        // 设置缓存数据
        job.addCacheFile(new URI("file:///C:/workspace/idea/springboot/hadoop-demo/src/main/resources/input/tablecache/pd.txt"));

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
