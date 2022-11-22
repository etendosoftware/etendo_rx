package com.etendorx.asyncprocess.serdes;

import java.util.Map;

import org.apache.commons.lang3.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import com.etendorx.lib.kafka.model.AsyncProcess;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AsyncProcessDeserializer implements Deserializer<AsyncProcess> {
  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
  }

  @Override
  public AsyncProcess deserialize(String topic, byte[] data) {
    try {
      if (data == null){
        System.out.println("Null received at deserializing");
        return null;
      }
      System.out.println("Deserializing...");
      return objectMapper.readValue(new String(data, "UTF-8"), AsyncProcess.class);
    } catch (Exception e) {
      throw new SerializationException("Error when deserializing byte[] to MessageDto");
    }
  }

  @Override
  public void close() {
  }
}
