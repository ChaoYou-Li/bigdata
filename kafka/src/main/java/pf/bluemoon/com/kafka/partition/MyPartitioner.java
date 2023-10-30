package pf.bluemoon.com.kafka.partition;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-10 09:12
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class MyPartitioner implements Partitioner {

    /**
     * 分区策略核心方法
     *
     * @param topic 主题
     * @param key
     * @param keyBytes
     * @param value
     * @param valueBytes
     * @param cluster
     * @return
     */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        //具体分区逻辑，这里全部发送到0号分区

        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
