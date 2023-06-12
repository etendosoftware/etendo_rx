package com.etendorx.das.connector.controllerLayer;

import com.etendorx.das.connector.kafka.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommunicationLayerService {

    @Value("${spring.kafka.modeling-topic}")
    private String modelingTopic;
    private final KafkaProducer kafkaProducer;

    @Autowired
    public CommunicationLayerService(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public void processEntity(String entity) {
        kafkaProducer.sendMessage(modelingTopic, entity);
    }
}
