package pf.bluemoon.com.hadoop.comparable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import pf.bluemoon.com.hadoop.serializable.FlowBean;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 20:54 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ComparableDriver {

    public static void drive(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        // 初始化配置参数
        Configuration config = new Configuration();
        Job job = Job.getInstance(config);

        // 关联Driver的jar类
        job.setJarByClass(ComparableDriver.class);

        // 关联Mapper/Reducer实现类
        job.setMapperClass(ComparableMapper.class);
        job.setReducerClass(ComparableReducer.class);

        // 设置Mapper的输出类型
        job.setMapOutputKeyClass(FlowBean.class);
        job.setMapOutputValueClass(Text.class);

        // 设置最后输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(FlowBean.class);

        // 关联自定义分区实现类
        job.setPartitionerClass(CustomPartitioner.class);
        // 设置ReduceTask数量
        job.setNumReduceTasks(5);

        // 设置输入和输出路径
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // 7、提交
        boolean completion = job.waitForCompletion(true);
        System.exit(completion ? 0 : 1);
    }

}
