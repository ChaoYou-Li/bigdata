package pf.bluemoon.com.hadoop.worldcount;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 14:46 2022/8/8
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class HelloReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
    private int sum;
    private IntWritable outValue = new IntWritable();

    @Override
    protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        sum = 0;

        // 累加
        for (IntWritable value : values) {
            sum += value.get();
        }
        outValue.set(sum);

        // 输出
        context.write(key, outValue);
    }
}
