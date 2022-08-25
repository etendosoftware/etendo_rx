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

import com.etendorx.lib.kafka.model.ProcessModel;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

public class EtendoTopology<O, D extends ProcessModel<O, D>> extends EtendoTopologyBase<O, D> {

  private final StreamsBuilder streamsBuilder;
  private final String processTopic;
  private final String destinationTopic;
  private final String sourceTopic;
  private final Serde<O> originSerde;
  private final Serde<D> destinationSerde;
  private KStream<String, O> originStream;
  private KStream<String, D> destinationStream;
  private KGroupedStream<String, O> group;

  @Value("${debezium_namespace,default}")
  String debeziumNamespace;

  public EtendoTopology(
      StreamsBuilder streamsBuilder,
      String sourceTopic, String destinationTopic, String processTopic,
      Serde<O> originSerde, Serde<D> destinationSerde,
      Supplier<D> newInstanceSupplier) {
    this.streamsBuilder = streamsBuilder;
    this.sourceTopic = sourceTopic;
    this.destinationTopic = destinationTopic;
    this.processTopic = processTopic;
    this.originSerde = originSerde;
    this.destinationSerde = destinationSerde;
    this.setNewInstanceSupplier(newInstanceSupplier);
  }

  public EtendoTopology<O, D> build() {
    this.originStream = streamsBuilder
        .stream(sourceTopic, Consumed.with(Serdes.String(), originSerde));
    this.groupByKey();
    return this;
  }

  private EtendoTopology<O, D> groupByKey() {
    this.group = this.originStream.groupByKey();
    return this;
  }

  public EtendoTopology<O, D> aggregate() {
    KTable<String, D> aggr = group.aggregate(
        getInitializer(),
        getAggregator(),
        materialized(destinationSerde, processTopic)
    );
    this.destinationStream = aggr.toStream();
    return this;
  }

  public void bind(StreamsBuilder streamsBuilder) {
    this.destinationStream
        .to(this.destinationTopic, Produced.with(Serdes.String(), destinationSerde));
  }

}
