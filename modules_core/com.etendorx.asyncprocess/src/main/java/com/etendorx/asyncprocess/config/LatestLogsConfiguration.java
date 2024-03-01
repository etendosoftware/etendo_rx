package com.etendorx.asyncprocess.config;

import com.etendorx.asyncprocess.serdes.PriorityQueueSerde;
import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.PriorityQueue;
import java.util.Properties;

import static com.etendorx.lib.kafka.topology.AsyncProcessTopology.ASYNC_PROCESS;

@Configuration
public class LatestLogsConfiguration {
  public static final String ASYNC_PROCESS_QUEUE = "async-process-queue5";
  public static final String ASYNC_PROCESS_STORE_QUEUE = "async-process-queue-store5";

  @Value("${kafka.streams.host.info:localhost:8080}")
  private String kafkaStreamsHostInfo;
  @Value("${kafka.streams2.state.dir:/tmp/kafka-streams/async-process-queue}")
  private String kafkaStreamsStateDir;
  @Value("${bootstrap_server:kafka:9092}")
  private String bootstrapServer;

  static class AsyncProcessComparator implements java.util.Comparator<AsyncProcess> {
    @Override
    public int compare(AsyncProcess o1, AsyncProcess o2) {
      return o2.getLastUpdate().compareTo(o1.getLastUpdate());
    }
  }

  public static void lastRecords(Properties properties, StreamsBuilder streamsBuilder) {

    Serde<AsyncProcess> asyncProcessSerde = new JsonSerde<>(AsyncProcess.class);

    streamsBuilder.stream(ASYNC_PROCESS, Consumed.with(Serdes.String(), asyncProcessSerde))
        .groupBy((s, asyncProcess) -> "1")
        .aggregate(() -> new PriorityQueue<>(1,
            (o1, o2) -> {
              if(o2.getLastUpdate().getTime() > o1.getLastUpdate().getTime()) {
                return 1;
              }
              if(o2.getLastUpdate().getTime() < o1.getLastUpdate().getTime()) {
                return -1;
              }
              return 0;
            }) , (key, value, list) -> {
          if(value.getId() == null) {
            return list;
          }
          list.removeIf(existingValue -> existingValue.getId().equals(value.getId()));
          value.getExecutions().clear();
          list.add(value);
          if (list.size() > 100) {
            list.poll();
          }
          return list;
        }, Materialized.<String, PriorityQueue<AsyncProcess>, KeyValueStore<Bytes, byte[]>>as(
                ASYNC_PROCESS_STORE_QUEUE)
            .withKeySerde(Serdes.String())
            .withValueSerde(
                new PriorityQueueSerde<>(new AsyncProcessComparator(), asyncProcessSerde)));

  }

}
