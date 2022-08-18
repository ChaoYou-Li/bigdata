package pf.bluemoon.com.hadoop.output;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 10:27 2022/8/14
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class LogMapper extends Mapper<LongWritable, Text, Text, NullWritable> {

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        context.write(value, NullWritable.get());
    }
}
