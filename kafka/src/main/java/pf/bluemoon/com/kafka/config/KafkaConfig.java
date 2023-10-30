package pf.bluemoon.com.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import pf.bluemoon.com.kafka.interceptor.ConsumerKafkaInterceptor;
import pf.bluemoon.com.kafka.interceptor.ProducerKafkaInterceptor;

import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-09 17:48
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<?, ?> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, ConsumerKafkaInterceptor.class.getName());
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    public ProducerFactory<?, ?> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        producerProperties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, ProducerKafkaInterceptor.class.getName());
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

}
