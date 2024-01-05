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

package com.etendorx.asyncprocess.topology;

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.etendorx.lib.kafka.model.JsonSerde;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncProcessTopologyTest {

  TopologyTestDriver testDriver;
  private TestInputTopic<String, AsyncProcessExecution> asyncProcessExecutionTopic;
  private TestOutputTopic<String, AsyncProcess> asyncProcessTopic;
  private TestOutputTopic<String, AsyncProcessExecution> rejectedProcessTopic;

  @BeforeEach
  void setup() {
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
    testDriver = new TopologyTestDriver(AsyncProcessTopology.buildTopology(), props);

    var asyncProcessJsonSerde = new JsonSerde<>(AsyncProcess.class);
    var asyncProcessExecutionJsonSerde = new JsonSerde<>(AsyncProcessExecution.class);

    asyncProcessExecutionTopic = testDriver.createInputTopic(
        AsyncProcessTopology.ASYNC_PROCESS_EXECUTION, Serdes.String().serializer(),
        asyncProcessExecutionJsonSerde.serializer());

    asyncProcessTopic = testDriver.createOutputTopic(AsyncProcessTopology.ASYNC_PROCESS,
        Serdes.String().deserializer(), asyncProcessJsonSerde.deserializer());
    rejectedProcessTopic = testDriver.createOutputTopic(AsyncProcessTopology.REJECTED_PROCESS,
        Serdes.String().deserializer(), asyncProcessExecutionJsonSerde.deserializer());
  }

  @AfterEach
  void teardown() {
    testDriver.close();
  }

  @Test
  void testTopology() {
    List.of(AsyncProcessExecution.builder().asyncProcessId("1").time(new Date()).build(),
            AsyncProcessExecution.builder().asyncProcessId("2").time(new Date()).build(),
            AsyncProcessExecution.builder().asyncProcessId("1").time(new Date()).build())
        .forEach(asyncProcessExecution -> asyncProcessExecutionTopic.pipeInput(
            asyncProcessExecution.getAsyncProcessId(), asyncProcessExecution));

    var firstAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("1", firstAsyncProcess.getId());

    var secondAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("2", secondAsyncProcess.getId());

    var thirdAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("1", thirdAsyncProcess.getId());

    assertTrue(rejectedProcessTopic.isEmpty());
  }

  @Test
  void testTopologyWhenRejection() {
    var rejectedTransactionId = UUID.randomUUID().toString();
    List.of(AsyncProcessExecution.builder()
            .id(rejectedTransactionId)
            .asyncProcessId("1")
            .time(new Date())
            .state(AsyncProcessState.REJECTED)
            .build(), AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("2")
            .time(new Date())
            .build(), AsyncProcessExecution.builder()
            .id(UUID.randomUUID().toString())
            .asyncProcessId("1")
            .time(new Date())
            .build())
        .forEach(asyncProcessExecution -> asyncProcessExecutionTopic.pipeInput(
            asyncProcessExecution.getAsyncProcessId(), asyncProcessExecution));

    var firstAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("1", firstAsyncProcess.getId());

    var secondAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("2", secondAsyncProcess.getId());

    var thirdAsyncProcess = asyncProcessTopic.readValue();

    assertEquals("1", thirdAsyncProcess.getId());

    var asyncProcessExecution = rejectedProcessTopic.readValue();

    assertEquals(rejectedTransactionId, asyncProcessExecution.getId());
    assertEquals(AsyncProcessState.REJECTED, asyncProcessExecution.getState());
  }

  @Test
  void testAsyncProcessTopologyWhenDone() {

    var asyncProcessId = UUID.randomUUID().toString();

    var asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.ACCEPTED)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var acceptedStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.ACCEPTED, acceptedStateProcess.getState());

    asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.WAITING)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var waitingStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.WAITING, waitingStateProcess.getState());

    asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.DONE)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var doneStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.DONE, doneStateProcess.getState());

  }

  @Test
  void testAsyncProcessTopologyWhenRejection() {
    var asyncProcessId = UUID.randomUUID().toString();

    var asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.ACCEPTED)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var acceptedStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.ACCEPTED, acceptedStateProcess.getState());

    asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.WAITING)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var waitingStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.WAITING, waitingStateProcess.getState());

    asyncProcessExecution = AsyncProcessExecution.builder()
        .id(asyncProcessId)
        .asyncProcessId("1")
        .time(new Date())
        .state(AsyncProcessState.REJECTED)
        .build();
    asyncProcessExecutionTopic.pipeInput(asyncProcessExecution.getAsyncProcessId(),
        asyncProcessExecution);

    var doneStateProcess = asyncProcessTopic.readValue();

    assertEquals(AsyncProcessState.REJECTED, doneStateProcess.getState());

  }

}
