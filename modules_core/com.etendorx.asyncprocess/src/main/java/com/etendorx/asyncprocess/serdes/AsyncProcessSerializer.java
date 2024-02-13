package com.etendorx.asyncprocess.serdes;

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.kafka.common.serialization.Serializer;

public class AsyncProcessSerializer implements Serializer<AsyncProcess> {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  @Override
  public byte[] serialize(String topic, AsyncProcess data) {
    return objectMapper.writeValueAsBytes(data);
  }

}
