package pf.bluemoon.com.hadoop.comparable;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import pf.bluemoon.com.hadoop.serializable.FlowBean;
import pf.bluemoon.com.hadoop.utils.NumberUtil;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 20:55 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ComparableMapper extends Mapper<LongWritable, Text, FlowBean, Text> {

    private FlowBean outKey = new FlowBean();

    private Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        if (null == line || line.length() < 1){
            return;
        }
        String[] worlds = line.split("\t");

        // 提取出上行流量
        String upFlow = worlds[worlds.length - 3];
        outKey.setUpFlow(NumberUtil.isNumber(upFlow) ? Long.parseLong(upFlow) : 0);
        // 提取下行流量
        String downFlow = worlds[worlds.length - 2];
        outKey.setDownFlow(NumberUtil.isNumber(downFlow) ? Long.parseLong(downFlow) : 0);
        // 统计总流量
        outKey.setSumFlow(outKey.getUpFlow() + outKey.getDownFlow());
        // 提取手机号
        outValue.set(worlds[1]);
        // map输出
        context.write(outKey, outValue);
    }

}
