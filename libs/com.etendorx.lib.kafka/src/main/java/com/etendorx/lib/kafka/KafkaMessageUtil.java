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

package com.etendorx.lib.kafka;

import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

import static com.etendorx.lib.kafka.model.JsonSerde.OBJECT_MAPPER;

@Component
@Slf4j
public class KafkaMessageUtil {

  private final KafkaProducer<String, String> producer;

  public KafkaMessageUtil(KafkaProducer<String, String> producer) {
    this.producer = producer;
  }

  private void save(AsyncProcessExecution asyncProcessExecution) {
    asyncProcessExecution.setTime(new Date());
    asyncProcessExecution.setId(UUID.randomUUID().toString());
    send(producer, new ProducerRecord<>(AsyncProcessTopology.ASYNC_PROCESS_EXECUTION,
        asyncProcessExecution.getAsyncProcessId(), toJson(asyncProcessExecution)));
  }

  @SneakyThrows
  private static void send(KafkaProducer<String, String> asyncProcessExecutionProducer,
      ProducerRecord<String, String> record) {
    log.debug("send {} {}", record.key(), record.value());
    asyncProcessExecutionProducer.send(record).get();
  }

  @SneakyThrows
  private static String toJson(AsyncProcessExecution asyncProcessExecution) {
    return OBJECT_MAPPER.writeValueAsString(asyncProcessExecution);
  }

  public void saveProcessExecution(Object bodyChanges, String mid, String description,
      AsyncProcessState state) {
    AsyncProcessExecution process = AsyncProcessExecution.builder()
        .asyncProcessId(mid)
        .description(description)
        .params(bodyChanges != null ? bodyChanges.toString() : "")
        .time(new Date())
        .state(state)
        .build();
    this.save(process);
  }

}
