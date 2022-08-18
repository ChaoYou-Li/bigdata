package pf.bluemoon.com.hadoop.output;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 0:00 2022/8/14
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class LogRecordWriter extends RecordWriter<Text, NullWritable> {

    private FSDataOutputStream atguiguOut;
    private FSDataOutputStream otherOut;

    /**
     * 初始化输出流对象
     */
    public LogRecordWriter(TaskAttemptContext job) {
        try {
            // 初始化配置参数
            FileSystem fs = FileSystem.get(job.getConfiguration());
            // 初始化输出流对象
            atguiguOut = fs.create(new Path("D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\out\\atguigu"));
            otherOut = fs.create(new Path("D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\out\\other"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 重写输出逻辑
     */
    @Override
    public void write(Text key, NullWritable value) {
        try {
            String log = key.toString();
            if (null == log || log.length() < 1){
                return;
            }
            if (log.contains("atguigu")){
                atguiguOut.writeBytes(log + "\n");
            } else {
                otherOut.writeBytes(log + "\n");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 关闭输出流对象
     */
    @Override
    public void close(TaskAttemptContext context) throws IOException {
        if (null != atguiguOut){
            atguiguOut.close();
        }
        if (null != otherOut){
            otherOut.close();
        }
    }

}
