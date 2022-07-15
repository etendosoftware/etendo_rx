package com.etendorx.integrations.asyncprocess.topology;

import com.etendorx.integrations.asyncprocess.model.AsyncProcess;
import com.etendorx.integrations.asyncprocess.model.AsyncProcessExecution;
import com.etendorx.integrations.asyncprocess.model.AsyncProcessState;
import com.etendorx.integrations.asyncprocess.model.JsonSerde;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;

public class AsyncProcessTopology {

  public static final String ASYNC_PROCESS_EXECUTION = "async-process-execution";
  public static final String ASYNC_PROCESS = "async-process";
  public static final String REJECTED_PROCESS = "rejected-process";
  public static final String ASYNC_PROCESS_STORE = "async-process-store";

  public static Topology buildTopology() {
    Serde<AsyncProcessExecution> asyncProcessExecutionSerdes = new JsonSerde<>(AsyncProcessExecution.class);
    Serde<AsyncProcess> asyncProcessSerde = new JsonSerde<>(AsyncProcess.class);
    StreamsBuilder streamsBuilder = new StreamsBuilder();

    KStream<String, AsyncProcess> asyncProcessExecutionStream = streamsBuilder.stream(ASYNC_PROCESS_EXECUTION,
            Consumed.with(Serdes.String(), asyncProcessExecutionSerdes))
        .groupByKey()
        .aggregate(AsyncProcess::new,
            (key, value, aggregate) -> aggregate.process(value),
            Materialized.<String, AsyncProcess, KeyValueStore<Bytes, byte[]>>as(ASYNC_PROCESS_STORE)
                .withKeySerde(Serdes.String())
                .withValueSerde(asyncProcessSerde)
        )
        .toStream();

    asyncProcessExecutionStream
        .to(ASYNC_PROCESS, Produced.with(Serdes.String(), asyncProcessSerde));

    asyncProcessExecutionStream
        .mapValues((readOnlyKey, value) -> value.getExecutions().first())
        .filter((key, value) -> value.getState() == AsyncProcessState.REJECTED)
        .to(REJECTED_PROCESS, Produced.with(Serdes.String(), asyncProcessExecutionSerdes));

    return streamsBuilder.build();
  }
}
