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

import org.apache.kafka.common.serialization.Serializer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * This class is a custom serializer for Deque objects.
 * It implements the Serializer interface provided by Apache Kafka.
 *
 * @param <T> The type of objects in the Deque.
 */
public class DequeSerializer<T> implements Serializer<Deque<T>> {

  // The serializer for the objects in the Deque.
  private final Serializer<T> valueSerializer;

  /**
   * Constructor for the DequeSerializer class.
   *
   * @param valueSerializer The serializer for the objects in the Deque.
   */
  public DequeSerializer(final Serializer<T> valueSerializer) {
    this.valueSerializer = valueSerializer;
  }

  /**
   * This method is used to configure the serializer.
   * It is not used in this implementation.
   *
   * @param configs The configuration for the serializer.
   * @param isKey Whether the serializer is used for keys or values.
   */
  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    // do nothing
  }

  /**
   * This method is used to serialize a Deque object into a byte array.
   *
   * @param topic The topic that the data is coming from.
   * @param queue The Deque object to be serialized.
   * @return byte[] The serialized byte array.
   */
  @Override
  public byte[] serialize(final String topic, final Deque<T> queue) {
    final int size = queue.size();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(baos);
    final Iterator<T> iterator = queue.iterator();
    try {
      out.writeInt(size);
      while (iterator.hasNext()) {
        final byte[] bytes = valueSerializer.serialize(topic, iterator.next());
        out.writeInt(bytes.length);
        out.write(bytes);
      }
      out.close();
    } catch (final IOException e) {
      throw new RuntimeException("unable to serialize PriorityQueue", e);
    }
    return baos.toByteArray();
  }

  /**
   * This method is used to close the serializer.
   * It is not used in this implementation.
   */
  @Override
  public void close() {

  }
}
