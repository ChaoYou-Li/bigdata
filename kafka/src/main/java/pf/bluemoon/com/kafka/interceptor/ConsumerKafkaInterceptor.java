package pf.bluemoon.com.kafka.interceptor;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-09 13:31
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ConsumerKafkaInterceptor implements ConsumerInterceptor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerKafkaInterceptor.class);

    @Override
    public void configure(Map<String, ?> configs) {
        // 在这里进行相关配置
    }

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        // 在这里处理消息被消费之前的逻辑
        // 返回经过处理的ConsumerRecords对象
        logger.info("============================ consumer：在这里处理消息被消费之前的逻辑 ============================");
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 在这里处理消费者提交位移之前的逻辑
        logger.info("============================ consumer：在这里处理消费者提交位移之前的逻辑 ============================");
    }

    @Override
    public void close() {
        // 在这里进行资源释放
    }

}
