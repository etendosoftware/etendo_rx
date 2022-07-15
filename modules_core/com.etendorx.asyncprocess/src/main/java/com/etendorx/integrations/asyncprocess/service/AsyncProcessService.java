package com.etendorx.integrations.asyncprocess.service;

import com.etendorx.integrations.asyncprocess.model.AsyncProcess;
import com.etendorx.integrations.asyncprocess.topology.AsyncProcessTopology;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

  private ReadOnlyKeyValueStore<String, AsyncProcess> getStore() {
    return kafkaStreams.store(
        StoreQueryParameters.fromNameAndType(
            AsyncProcessTopology.ASYNC_PROCESS_STORE,
            QueryableStoreTypes.keyValueStore()));
  }
}
