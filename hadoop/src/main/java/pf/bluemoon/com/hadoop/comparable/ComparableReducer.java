package pf.bluemoon.com.hadoop.comparable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import pf.bluemoon.com.hadoop.serializable.FlowBean;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 21:00 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ComparableReducer extends Reducer<FlowBean, Text, Text, FlowBean> {

    @Override
    protected void reduce(FlowBean key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        //遍历values集合,循环写出,避免总流量相同的情况
        for (Text value : values) {
            //调换KV位置,反向写出
            context.write(value,key);
        }
    }
}
