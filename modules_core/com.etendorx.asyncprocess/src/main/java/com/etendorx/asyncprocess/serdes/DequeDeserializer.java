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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.*;

/**
 * This class is a custom deserializer for Deque objects.
 * It implements the Deserializer interface provided by Apache Kafka.
 *
 * @param <T> The type of objects in the Deque.
 */
public class DequeDeserializer<T> implements Deserializer<Deque<T>> {

  // The deserializer for the objects in the Deque.
  private final Deserializer<T> valueDeserializer;

  /**
   * Constructor for the DequeDeserializer class.
   *
   * @param valueDeserializer The deserializer for the objects in the Deque.
   */
  public DequeDeserializer(final Deserializer<T> valueDeserializer) {
    this.valueDeserializer = valueDeserializer;
  }

  /**
   * This method is used to configure the deserializer.
   * It is not used in this implementation.
   *
   * @param configs The configuration for the deserializer.
   * @param isKey Whether the deserializer is used for keys or values.
   */
  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    // do nothing
  }

  /**
   * This method is used to deserialize a byte array into a Deque object.
   *
   * @param s The topic that the data is coming from.
   * @param bytes The byte array to be deserialized.
   * @return Deque<T> The deserialized Deque object.
   */
  @Override
  public Deque<T> deserialize(final String s, final byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    final Deque<T> priorityQueue = new ArrayDeque<>();
    final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
    try {
      final int records = dataInputStream.readInt();
      for (int i = 0; i < records; i++) {
        final byte[] valueBytes = new byte[dataInputStream.readInt()];
        if (dataInputStream.read(valueBytes) != valueBytes.length) {
          throw new BufferUnderflowException();
        }
        priorityQueue.add(valueDeserializer.deserialize(s, valueBytes));
      }
    } catch (final IOException e) {
      throw new RuntimeException("Unable to deserialize PriorityQueue", e);
    }
    return priorityQueue;
  }

  /**
   * This method is used to close the deserializer.
   * It is not used in this implementation.
   */
  @Override
  public void close() {

  }
}
