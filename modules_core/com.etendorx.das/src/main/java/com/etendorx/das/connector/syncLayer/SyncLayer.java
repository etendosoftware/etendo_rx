package com.etendorx.das.connector.syncLayer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;

public class SyncLayer {

    @KafkaListener(topics = "sync-topic}", groupId = "1")
    public void consume(String message) {
        processMessage(message);
    }

    private void processMessage(String message) {
    }

}
