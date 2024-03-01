package com.etendorx.asyncprocess.serdes;

import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.Map;
import java.util.PriorityQueue;

public class PriorityQueueDeserializer<T> implements Deserializer<PriorityQueue<T>> {

  private final Deserializer<T> valueDeserializer;

  public PriorityQueueDeserializer(final Deserializer<T> valueDeserializer) {
    this.valueDeserializer = valueDeserializer;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    // do nothing
  }

  @Override
  public PriorityQueue<T> deserialize(final String s, final byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    final PriorityQueue<T> priorityQueue = new PriorityQueue<>();
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

  @Override
  public void close() {

  }
}
