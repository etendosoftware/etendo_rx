/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.asyncprocess.config;

import com.etendorx.asyncprocess.serdes.PriorityQueueSerde;
import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.etendorx.lib.kafka.model.JsonSerde;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayDeque;
import java.util.Deque;

import static com.etendorx.lib.kafka.topology.AsyncProcessTopology.ASYNC_PROCESS;

/**
 * This class is responsible for configuring the latest logs for asynchronous processes.
 * It uses the Kafka Streams API to consume and aggregate the logs.
 */
@Configuration
public class LatestLogsConfiguration {

  private LatestLogsConfiguration() {
    // Private constructor to hide the implicit public one.
  }

  // The name of the Kafka store for the asynchronous process queue.
  public static final String ASYNC_PROCESS_STORE_QUEUE = "async-process-queue-store5";

  /**
   * This method is used to consume and aggregate the latest logs for asynchronous processes.
   * It uses the Kafka Streams API to consume the logs from a specific topic and aggregate them into a queue.
   *
   * @param streamsBuilder The StreamsBuilder object used to build the Kafka Streams topology.
   */
  public static void lastRecords(StreamsBuilder streamsBuilder) {

    // The Serde object used for serialization and deserialization of AsyncProcess objects.
    Serde<AsyncProcess> asyncProcessSerde = new JsonSerde<>(AsyncProcess.class);

    // Consuming and aggregating the logs from the ASYNC_PROCESS topic into a queue.
    streamsBuilder.stream(ASYNC_PROCESS, Consumed.with(Serdes.String(), asyncProcessSerde))
        .groupBy((s, asyncProcess) -> "1")
        .aggregate(ArrayDeque::new, (key, value, list) -> updateList(value, list),
            Materialized.<String, Deque<AsyncProcess>, KeyValueStore<Bytes, byte[]>>as(ASYNC_PROCESS_STORE_QUEUE)
            .withKeySerde(Serdes.String())
            .withValueSerde(new PriorityQueueSerde<>(asyncProcessSerde)));
  }

  /**
   * This method is used to update the list of logs for an asynchronous process.
   * It removes any existing logs with the same ID as the new log and adds the new log to the front of the list.
   * If the list size exceeds 100, it removes the oldest log from the list.
   *
   * @param value The new log for the asynchronous process.
   * @param list The current list of logs for the asynchronous process.
   * @return Deque<AsyncProcess> The updated list of logs for the asynchronous process.
   */
  private static Deque<AsyncProcess> updateList(AsyncProcess value, Deque<AsyncProcess> list) {
    if (StringUtils.isEmpty(value.getId()) || value.getState().equals(AsyncProcessState.ACCEPTED)) {
      return list;
    }
    list.removeIf(existingValue -> existingValue.getId().equals(value.getId()));
    value.getExecutions().clear();
    list.addFirst(value);
    if (list.size() > 100) {
      list.removeLast();
    }
    return list;
  }
}
