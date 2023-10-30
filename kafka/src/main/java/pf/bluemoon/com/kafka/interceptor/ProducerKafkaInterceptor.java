package pf.bluemoon.com.kafka.interceptor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-09 17:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ProducerKafkaInterceptor implements ProducerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ProducerKafkaInterceptor.class);

    @Override
    public ProducerRecord onSend(ProducerRecord record) {
        // 在发送消息之前对消息进行处理
        logger.info("============================ producer：在发送消息之前对消息进行处理 ============================");
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // 消息被确认或发送失败时的回调方法
        logger.info("============================ producer：消息被确认或发送失败时的回调方法 ============================");
    }

    @Override
    public void close() {
        // 关闭拦截器时的清理操作
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // 拦截器的配置方法
    }
}
