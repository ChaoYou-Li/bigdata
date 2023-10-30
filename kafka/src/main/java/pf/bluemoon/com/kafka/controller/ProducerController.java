package pf.bluemoon.com.kafka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

/**
 * @Author chaoyou
 * @Date Create in 2023-10-10 11:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@RestController
public class ProducerController {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    @PostMapping
    public
}
