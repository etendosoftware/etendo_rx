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

package com.etendorx.asyncprocess;

import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class AsyncProcessDbProducer {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static void main(String[] args) {
    KafkaProducer<String, String> asyncProcessExecutionProducer =
        new KafkaProducer<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));

    List<AsyncProcessExecution> data1 = List.of(
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("1")
            .time(new Date())
            .log("Incomme")
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("2")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("3")
            .log("Amazon")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("4")
            .log("Rent")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("5")
            .log("Electricity")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("6")
            .log("Wallmart")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("7")
            .log("Vodafone")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("8")
            .log("Amazon")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("9")
            .log("Netflix")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("10")
            .log("Transport")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("11")
            .log("Transport")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("12")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("13")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("14")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("15")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("16")
            .time(new Date())
            .build(),
        AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("17")
            .time(new Date())
            .build()
    );
    List<AsyncProcessState> stateList = List.of(
        AsyncProcessState.ACCEPTED, AsyncProcessState.WAITING, AsyncProcessState.DONE
    );

    stateList.forEach(state -> {
      data1.stream()
          .map(asyncProcessExecution -> {
            asyncProcessExecution.setId(UUID.randomUUID().toString());
            asyncProcessExecution.setState(state);
            asyncProcessExecution.setLog(asyncProcessExecution.getLog() + " " + asyncProcessExecution.getState().toString());
            return new ProducerRecord<>("async-process-execution", asyncProcessExecution.getAsyncProcessId(), toJson(asyncProcessExecution));
          }).forEach(record -> {
            try {
              Thread.sleep(2000);
              send(asyncProcessExecutionProducer, record);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          });
    });

    AsyncProcessExecution asyncProcessExecution = AsyncProcessExecution.builder()
        .id(UUID.randomUUID().toString())
        .asyncProcessId("3")
        .time(new Date())
        .build();

    send(asyncProcessExecutionProducer, new ProducerRecord<>("async-process-execution", asyncProcessExecution.getAsyncProcessId(), toJson(asyncProcessExecution)));

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
