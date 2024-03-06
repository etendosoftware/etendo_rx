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
package com.etendorx.asyncprocess.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Deque;
import java.util.Map;

/**
 * This class is a custom Serde (Serializer/Deserializer) for Deque objects.
 * It implements the Serde interface provided by Apache Kafka.
 *
 * @param <T> The type of objects in the Deque.
 */
public class PriorityQueueSerde<T> implements Serde<Deque<T>> {

  // The inner Serde object used for serialization and deserialization of Deque objects.
  private final Serde<Deque<T>> inner;

  /**
   * Constructor for the PriorityQueueSerde class.
   *
   * @param avroSerde The Avro Serde object used for serialization and deserialization of objects in the Deque.
   */
  public PriorityQueueSerde(final Serde<T> avroSerde) {
    inner = Serdes.serdeFrom(new DequeSerializer<>(avroSerde.serializer()),
        new DequeDeserializer<>(avroSerde.deserializer()));
  }

  /**
   * This method is used to get the serializer for the Deque objects.
   *
   * @return Serializer<Deque<T>> The serializer for the Deque objects.
   */
  @Override
  public Serializer<Deque<T>> serializer() {
    return inner.serializer();
  }

  /**
   * This method is used to get the deserializer for the Deque objects.
   *
   * @return Deserializer<Deque<T>> The deserializer for the Deque objects.
   */
  @Override
  public Deserializer<Deque<T>> deserializer() {
    return inner.deserializer();
  }

  /**
   * This method is used to configure the Serde.
   *
   * @param configs The configuration for the Serde.
   * @param isKey Whether the Serde is used for keys or values.
   */
  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    inner.serializer().configure(configs, isKey);
    inner.deserializer().configure(configs, isKey);
  }

  /**
   * This method is used to close the Serde.
   */
  @Override
  public void close() {
    inner.serializer().close();
    inner.deserializer().close();
  }
}
