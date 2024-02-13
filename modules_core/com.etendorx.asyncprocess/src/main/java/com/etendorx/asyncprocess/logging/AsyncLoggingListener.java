package com.etendorx.asyncprocess.logging;

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Log4j2
@Component
public class AsyncLoggingListener {
  @Value("${das.url:}")
  String dasUrl;
  @Value("${token:}")
  String token;

  @KafkaListener(topics = AsyncProcessTopology.ASYNC_PROCESS, groupId = "async-process-logging" )
  public void onAsyncProcessExecution(String asyncProcessExecution) throws JsonProcessingException {
    log.info("Received async process execution: {}", asyncProcessExecution);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
    AsyncProcess asyncProcess = objectMapper.readValue(asyncProcessExecution, AsyncProcess.class);
    log.info("Received async process execution: {}", asyncProcess);
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.set("Authorization", "Bearer " + token);
    // Entity to send
    HttpEntity<AsyncProcess> httpEntity = new HttpEntity<>(asyncProcess, headers);
    // check if exists
    ResponseEntity<String> status = null;
    try {
      status = restTemplate.exchange(dasUrl + "/connector/asyncProcess/" + asyncProcess.getId() + "?_dateFormat=dd-MM-yyyy HH:mm:ss",
          HttpMethod.HEAD, new HttpEntity<>(headers), String.class, httpEntity);
    } catch (Exception e) {
    }
    ResponseEntity<String> response;
    if (status != null && status.getStatusCode().is2xxSuccessful()) {
      response = restTemplate.exchange(dasUrl + "/connector/asyncProcess/" + asyncProcess.getId() + "?_dateFormat=dd-MM-yyyy HH:mm:ss&_timeZone=UTC-3",
          HttpMethod.PUT, httpEntity, String.class, httpEntity);
    } else {
      response = restTemplate.exchange(dasUrl + "/connector/asyncProcess?_dateFormat=dd-MM-yyyy HH:mm:ss&_timeZone=UTC-3", HttpMethod.POST,
          httpEntity, String.class, httpEntity);
    }
    if (response.getStatusCode().is2xxSuccessful()) {
      int lineno = asyncProcess.getExecutions().size() * 10;
      for (AsyncProcessExecution execution : asyncProcess.getExecutions()) {
        execution.setLineno(lineno);
        String payload = "";
        try {
          payload = objectMapper.writeValueAsString(execution);
        } catch (JsonProcessingException e) {
          log.error("Error processing async process execution: {}", e.getMessage());
        }
        log.info("Payload: {}", payload);
        HttpEntity<String> httpEntityExec = new HttpEntity<>(payload, headers);
        log.info("Sending async process execution to DAS: {}", execution);
        try {
          var execResult = restTemplate.exchange(dasUrl + "/connector/asyncProcessExec?_dateFormat=dd-MM-yyyy HH:mm:ss&_timeZone=UTC-3",
              HttpMethod.POST, httpEntityExec, String.class);
          if (!execResult.getStatusCode().is2xxSuccessful()) {
            log.warn("HttpCode {} sending async process execution to DAS: {}",
                execResult.getStatusCode(), execResult);
          }
        } catch (Exception e) {
          log.warn("Error sending async process execution to DAS: {}", e.getMessage());
        }
        lineno -= 10;
      }
    } else {
      log.error("Error sending async process execution to DAS: {}", response);
    }
    log.info("Response: {}", response);

  }
}
