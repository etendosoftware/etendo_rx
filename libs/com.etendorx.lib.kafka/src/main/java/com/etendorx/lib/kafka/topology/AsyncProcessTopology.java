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

package com.etendorx.lib.kafka.topology;

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.etendorx.lib.kafka.model.AsyncProcessExecution;
import com.etendorx.lib.kafka.model.AsyncProcessState;
import com.etendorx.lib.kafka.model.JsonSerde;
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
