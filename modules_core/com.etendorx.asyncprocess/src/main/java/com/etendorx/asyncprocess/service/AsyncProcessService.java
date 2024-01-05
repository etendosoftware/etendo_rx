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

/**
 * Service that returns information from a process stored in Kafka.
 */
@Service
public class AsyncProcessService {

  private final KafkaStreams kafkaStreams;
  private final HostInfo hostInfo;

  @Autowired
  public AsyncProcessService(KafkaStreams kafkaStreams, HostInfo hostInfo) {
    this.kafkaStreams = kafkaStreams;
    this.hostInfo = hostInfo;
  }

  public AsyncProcess getAsyncProcess(String asyncProcessId) {
    return getStore().get(asyncProcessId);
  }

  public ReadOnlyKeyValueStore<String, AsyncProcess> getStore() {
    return kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(AsyncProcessTopology.ASYNC_PROCESS_STORE,
            QueryableStoreTypes.keyValueStore()));
  }

}
