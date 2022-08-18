package pf.bluemoon.com.hadoop.serializable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 10:14 2022/8/9
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FlowReducer extends Reducer<Text, FlowBean, Text, FlowBean> {
    private static final Logger logger = LoggerFactory.getLogger(FlowReducer.class);
    private FlowBean outValue = new FlowBean();

    @Override
    protected void reduce(Text key, Iterable<FlowBean> values, Context context) throws IOException, InterruptedException {
        long upTotal = 0;
        long downTotal = 0;

        // 聚合计算
        for (FlowBean value : values) {
            upTotal += value.getUpFlow();
            downTotal += value.getDownFlow();
        }
        outValue.setUpFlow(upTotal);
        outValue.setDownFlow(downTotal);
        outValue.setSumFlow(upTotal + downTotal);

        // reduce输出
        context.write(key, outValue);
    }
}
