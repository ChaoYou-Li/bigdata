package pf.bluemoon.com.hadoop.output;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * @Author chaoyou
 * @Date Create in 23:59 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class LogOutputFormat extends FileOutputFormat<Text, NullWritable> {

    /**
     * 初始化自定义输出对象
     */
    @Override
    public RecordWriter<Text, NullWritable> getRecordWriter(TaskAttemptContext job) {
        return new LogRecordWriter(job);
    }
}
