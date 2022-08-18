package pf.bluemoon.com.hadoop.worldcount;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 14:51 2022/8/8
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class HelloDriver {
    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 1、获取配置信息以及获取job对象
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 2、关联Driver程序的jar
        job.setJarByClass(HelloDriver.class);

        // 3、关联Mapper和Reducer的jar
        job.setMapperClass(HelloMapper.class);
        job.setReducerClass(HelloReducer.class);

        // 4、设置Mapper输出的K/V类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        // 5、设置最终输出的类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // 6、设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);

    }
}
