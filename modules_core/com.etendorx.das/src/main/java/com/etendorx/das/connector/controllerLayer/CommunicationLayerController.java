package com.etendorx.das.connector.controllerLayer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommunicationLayerController {
    private final CommunicationLayerService communicationLayerService;

    public CommunicationLayerController(CommunicationLayerService communicationLayer) {
        this.communicationLayerService = communicationLayer;
    }

    @PostMapping("/entities")
    public ResponseEntity<String> receiveEntity(@RequestBody String entity) {
        communicationLayerService.processEntity(entity);
        return ResponseEntity.ok("Entity received successfully");
    }
}
