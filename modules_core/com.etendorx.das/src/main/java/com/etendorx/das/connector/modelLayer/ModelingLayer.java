package com.etendorx.das.connector.modelLayer;

import com.etendorx.das.connector.CustomErrors.modelLayer.ProcessingError;
import com.etendorx.das.connector.kafka.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ModelingLayer {

    private final KafkaProducer kafkaProducer;

    @Value("${spring.kafka.sync-topic}")
    private String syncTopic;

    @Value("${spring.kafka.sync-topic-error}")
    private String syncTopicError;

    public ModelingLayer(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @KafkaListener(topics = "modeling-topic", groupId = "1")
    public void consume(String message) {
        try {
            kafkaProducer.sendMessage(syncTopic, processMessage(message));
        } catch (ProcessingError e) {
            kafkaProducer.sendMessage(syncTopicError, message);
        }
    }

    private String processMessage(String message) throws ProcessingError {
        try {
            //TODO: process entity
            return message;
        } catch (Exception e) {
            log.info("[ModelingLayer] - Error processing the entity");
            throw new ProcessingError("[ModelingLayer] - Error processing the entity: {}", e.getMessage());
        }

    }
}
