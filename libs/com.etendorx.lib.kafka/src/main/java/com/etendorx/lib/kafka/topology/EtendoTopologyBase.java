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
import lombok.Setter;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.function.Supplier;

public abstract class EtendoTopologyBase<O, D extends ProcessModel<O, D>> {

  @Setter
  private Supplier<D> newInstanceSupplier;

  Aggregator<String, O, D> getAggregator() {
    return (key, value, aggregate) -> aggregate.process(value);
  }

  Initializer<D> getInitializer() {
    return () -> newInstanceSupplier.get();
  }

  Materialized<String, D, KeyValueStore<Bytes, byte[]>> materialized(Serde<D> destinationSerde, String processTopic) {
    return Materialized.<String, D, KeyValueStore<Bytes, byte[]>>as(processTopic)
        .withKeySerde(Serdes.String())
        .withValueSerde(destinationSerde);
  }

}
