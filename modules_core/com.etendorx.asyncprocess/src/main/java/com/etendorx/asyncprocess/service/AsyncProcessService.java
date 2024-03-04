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

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static com.etendorx.asyncprocess.config.LatestLogsConfiguration.ASYNC_PROCESS_STORE_QUEUE;

/**
 * This service class is responsible for retrieving information from a process stored in Kafka.
 * It uses the KafkaStreams and HostInfo objects to interact with the Kafka store.
 */
@Service
public class AsyncProcessService {

  // The KafkaStreams object used to interact with the Kafka store.
  private final KafkaStreams kafkaStreams;

  // The HostInfo object used to interact with the Kafka store.
  private final HostInfo hostInfo;

  /**
   * Constructor for the AsyncProcessService class.
   *
   * @param kafkaStreams The KafkaStreams object to be used.
   * @param hostInfo     The HostInfo object to be used.
   */
  @Autowired
  public AsyncProcessService(KafkaStreams kafkaStreams, HostInfo hostInfo) {
    this.kafkaStreams = kafkaStreams;
    this.hostInfo = hostInfo;
  }

  /**
   * Retrieves an AsyncProcess object from the Kafka store using its ID.
   *
   * @param asyncProcessId The ID of the AsyncProcess object to be retrieved.
   * @return AsyncProcess The retrieved AsyncProcess object.
   */
  public AsyncProcess getAsyncProcess(String asyncProcessId) {
    return getStore().get(asyncProcessId);
  }

  /**
   * Retrieves a ReadOnlyKeyValueStore object from the Kafka store.
   *
   * @return ReadOnlyKeyValueStore The retrieved ReadOnlyKeyValueStore object.
   */
  public ReadOnlyKeyValueStore<String, AsyncProcess> getStore() {
    return kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(AsyncProcessTopology.ASYNC_PROCESS_STORE,
            QueryableStoreTypes.keyValueStore()));
  }

  /**
   * Retrieves a list of the latest AsyncProcess objects from the Kafka store.
   *
   * @return List<AsyncProcess> The list of retrieved AsyncProcess objects.
   */
  public List<AsyncProcess> getLatestAsyncProcesses() {
    ReadOnlyKeyValueStore<String, Deque<AsyncProcess>> store = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(ASYNC_PROCESS_STORE_QUEUE,
            QueryableStoreTypes.keyValueStore()));
    Deque<AsyncProcess> records = store.get("1");
    return records == null ? Collections.emptyList() : new ArrayList<>(records);
  }

}
