package pf.bluemoon.com.hadoop.partitition;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;
import pf.bluemoon.com.hadoop.serializable.FlowBean;

/**
 * @Author chaoyou
 * @Date Create in 17:17 2022/8/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class CustomPartitioner extends Partitioner<Text, FlowBean> {

    /**
     * 分区逻辑：共设置5个分区，根据手机号前3位设置分区号
     *      0：136
     *      1：137
     *      2：138
     *      3：139
     *      4：其他
     *
     * @param key
     * @param value
     * @param numPartitions
     * @return
     */
    @Override
    public int getPartition(Text key, FlowBean value, int numPartitions) {
        int partition;
        String phone = key.toString();
        String prePhone = phone.substring(0, 3);
        if ("136".equals(prePhone)){
            partition = 1;
        } else if ("137".equals(prePhone)){
            partition = 2;
        } else if ("138".equals(prePhone)){
            partition = 3;
        } else if ("139".equals(prePhone)){
            partition = 4;
        } else {
            partition = 0;
        }
        return partition;
    }
}
