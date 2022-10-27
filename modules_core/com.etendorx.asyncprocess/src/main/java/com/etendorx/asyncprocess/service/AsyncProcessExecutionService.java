/*
 * Copyright 2022  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.etendorx.asyncprocess.service;

import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
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

/**
 * Service used to simplify message delivery
 */
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
    send(producer,
        new ProducerRecord<>(AsyncProcessTopology.ASYNC_PROCESS_EXECUTION, asyncProcessExecution.getAsyncProcessId(),
            toJson(asyncProcessExecution)));
  }

  @SneakyThrows
  private static void send(KafkaProducer<String, String> asyncProcessExecutionProducer,
      ProducerRecord<String, String> record) {
    log.info("send {} {}", record.key(), record.value());
    asyncProcessExecutionProducer.send(record).get();
  }

  @SneakyThrows
  private static String toJson(AsyncProcessExecution asyncProcessExecution) {
    return OBJECT_MAPPER.writeValueAsString(asyncProcessExecution);
  }

}
