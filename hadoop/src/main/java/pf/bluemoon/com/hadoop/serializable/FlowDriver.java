package pf.bluemoon.com.hadoop.serializable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import pf.bluemoon.com.hadoop.partitition.PartitionDriver;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 20:46 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FlowDriver {

    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(PartitionDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(FlowMapper.class);
        job.setReducerClass(FlowReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(FlowBean.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);

    }

}
