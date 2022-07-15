package com.etendorx.integrations.asyncprocess.service;

import com.etendorx.integrations.asyncprocess.model.AsyncProcessExecution;
import com.etendorx.integrations.asyncprocess.topology.AsyncProcessTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class AsyncProcessExecutionService {

  private final KafkaProducer<String, String> producer;
  private final HostInfo hostInfo;
  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Autowired
  public AsyncProcessExecutionService(KafkaProducer<String, String> producer, HostInfo hostInfo) {
    this.producer = producer;
    this.hostInfo = hostInfo;
  }

  public void save(AsyncProcessExecution asyncProcessExecution) {
    asyncProcessExecution.setTime(new Date());
    asyncProcessExecution.setId(UUID.randomUUID().toString());
    send(producer, new ProducerRecord<>(AsyncProcessTopology.ASYNC_PROCESS_EXECUTION, asyncProcessExecution.getAsyncProcessId(), toJson(asyncProcessExecution)));
  }

  @SneakyThrows
  private static void send(KafkaProducer<String, String> asyncProcessExecutionProducer, ProducerRecord<String, String> record) {
    log.info("send {} {}", record.key(), record.value());
    asyncProcessExecutionProducer.send(record).get();
  }

  @SneakyThrows
  private static String toJson(AsyncProcessExecution asyncProcessExecution) {
    return OBJECT_MAPPER.writeValueAsString(asyncProcessExecution);
  }

}
