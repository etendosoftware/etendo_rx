package com.etendorx.integrations.asyncprocess.config;

import com.etendorx.integrations.asyncprocess.topology.AsyncProcessTopology;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Configuration
public class StreamConfiguration {

  @Value("${kafka.streams.host.info:localhost:8080}")
  private String kafkaStreamsHostInfo;
  @Value("${kafka.streams.state.dir:/tmp/kafka-streams/async-process-queries}")
  private String kafkaStreamsStateDir;


  @Bean
  public Properties kafkaStreamsConfiguration() {
    Properties properties = new Properties();
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "async-process-queries");
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
    properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
    properties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, kafkaStreamsHostInfo);
    properties.put(StreamsConfig.STATE_DIR_CONFIG, kafkaStreamsStateDir);
    return properties;
  }

  @Bean
  public KafkaStreams kafkaStreams(@Qualifier("kafkaStreamsConfiguration") Properties streamConfiguration) {
    var topology = AsyncProcessTopology.buildTopology();
    var kafkaStreams = new KafkaStreams(topology, streamConfiguration);

    kafkaStreams.cleanUp();
    kafkaStreams.start();

    Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

    return kafkaStreams;
  }

  @Bean
  public KafkaProducer<String, String> producer() {
    KafkaProducer<String, String> producer =
        new KafkaProducer<>(
            Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaStreamsConfiguration().get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            )
        );
    return producer;
  }

  @Bean
  public HostInfo hostInfo() {
    var split = kafkaStreamsHostInfo.split(":");
    return new HostInfo(split[0], Integer.parseInt(split[1]));
  }
}
