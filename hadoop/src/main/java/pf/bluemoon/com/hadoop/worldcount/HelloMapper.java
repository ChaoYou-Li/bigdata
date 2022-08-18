package pf.bluemoon.com.hadoop.worldcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 14:40 2022/8/8
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class HelloMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
    private Text outKey = new Text();
    private IntWritable outValue = new IntWritable(1);

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // 获取一行数据
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }

        // 切割行数据
        String[] worlds = line.split(" ");

        // 输出
        for (String world : worlds) {
            outKey.set(world);
            context.write(outKey, outValue);
        }
    }
}
