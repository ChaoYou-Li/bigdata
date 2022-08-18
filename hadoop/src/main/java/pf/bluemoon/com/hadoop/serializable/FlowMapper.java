package pf.bluemoon.com.hadoop.serializable;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pf.bluemoon.com.hadoop.utils.NumberUtil;

/**
 * @Author chaoyou
 * @Date Create in 9:24 2022/8/9
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FlowMapper extends Mapper<LongWritable, Text, Text, FlowBean> {

    private static final Logger logger = LoggerFactory.getLogger(FlowBean.class);

    private Text outKey = new Text();
    private FlowBean outValue = new FlowBean();

    @Override
    protected void map(LongWritable key, Text value, Context context) {
        try {
            // 获取行数据
            String line = value.toString();
            // 切割行数据
            String[] worlds = line.split("\t");
            // 提取出上行流量
            String upFlow = worlds[worlds.length - 3];
            outValue.setUpFlow(NumberUtil.isNumber(upFlow) ? Long.parseLong(upFlow) : 0);
            // 提取下行流量
            String downFlow = worlds[worlds.length - 2];
            outValue.setDownFlow(NumberUtil.isNumber(downFlow) ? Long.parseLong(downFlow) : 0);
            // 统计总流量
            outValue.setSumFlow(outValue.getUpFlow() + outValue.getDownFlow());
            // 提取手机号
            outKey.set(worlds[1]);
            // map输出
            context.write(outKey, outValue);
        } catch (Exception e){
            logger.error("map业务处理报错：{}, message：{}", value.toString(), e.getMessage());
            e.printStackTrace();
        }

    }
}
