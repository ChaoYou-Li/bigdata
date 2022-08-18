package pf.bluemoon.com.hadoop.output;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 10:21 2022/8/14
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class LogDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联driver驱动类jar
        job.setJarByClass(LogDriver.class);

        // 关联Mapper/Reducer类
        job.setMapperClass(LogMapper.class);
        job.setReducerClass(LogReducer.class);

        // 设置Mapper输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(NullWritable.class);

        // 设置最终输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // 设置自定义输出类
        job.setOutputFormatClass(LogOutputFormat.class);

        // 6、设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }
}
