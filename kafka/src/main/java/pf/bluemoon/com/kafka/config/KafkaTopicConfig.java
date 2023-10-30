package pf.bluemoon.com.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-10 11:09
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-server}")
    private String serverConfig;

    /**
     * 创建 KafkaAmin，可以自动检测集群中是否存在topic，不存在则创建
     * @return
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, serverConfig);
        return new KafkaAdmin(props);
    }

    @Bean
    public NewTopic newTopic() {
        // 创建 topic，指定 名称、分区数、副本数
        return new NewTopic("hello-kafka-test-topic", 3, (short) 2);
    }

}
