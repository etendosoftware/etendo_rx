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

package com.etendorx.lib.kafka.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerde<T> implements Serde<T> {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Class<T> type;

  public JsonSerde(Class<T> type) {
    this.type = type;
  }

  @Override
  public Serializer<T> serializer() {
    return (topic, data) -> serialize(data);
  }

  @SneakyThrows
  private byte[] serialize(T data) {
    return OBJECT_MAPPER.writeValueAsBytes(data);
  }

  @Override
  public Deserializer<T> deserializer() {
    return (topic, bytes) -> deserialize(bytes);
  }

  @SneakyThrows
  private T deserialize(byte[] bytes) {
    return OBJECT_MAPPER.readValue(bytes, type);
  }
}
