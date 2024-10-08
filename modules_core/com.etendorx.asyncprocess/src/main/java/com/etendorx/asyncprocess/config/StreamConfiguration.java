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

package com.etendorx.asyncprocess.config;

import com.etendorx.asyncprocess.serdes.AsyncProcessSerializer;
import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.internals.ConsumerFactory;
import reactor.kafka.receiver.internals.DefaultKafkaReceiver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.etendorx.lib.kafka.topology.AsyncProcessTopology.ASYNC_PROCESS;

/**
 * This class is responsible for configuring the Kafka Streams for the application.
 * It sets up the Kafka Streams configuration, creates the Kafka Streams instance, and starts it.
 * It also provides beans for the Kafka Receiver and Kafka Template for sending retries.
 */
@Configuration
public class StreamConfiguration {

  // The name of the Kafka Streams application.
  public static final String ASYNC_PROCESS_QUERIES = "async-process-queries";

  // The host information for the Kafka Streams application.
  @Value("${kafka.streams.host.info:localhost:8080}")
  private String kafkaStreamsHostInfo;

  // The directory for the Kafka Streams state store.
  @Value("${kafka.streams.state.dir:/tmp/kafka-streams/async-process-queries}")
  private String kafkaStreamsStateDir;

  // The bootstrap servers for Kafka.
  @Value("${bootstrap_server:kafka:9092}")
  private String bootstrapServer;

  /**
   * This method is used to set up the Kafka Streams configuration.
   *
   * @return Properties The Kafka Streams configuration.
   */
  @Bean
  public Properties kafkaStreamsConfiguration() {
    Properties properties = new Properties();
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, ASYNC_PROCESS_QUERIES);
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "true");
    properties.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, "0");
    properties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, kafkaStreamsHostInfo);
    properties.put(StreamsConfig.STATE_DIR_CONFIG, kafkaStreamsStateDir);
    return properties;
  }

  /**
   * This method is used to create and start the Kafka Streams instance.
   *
   * @param streamConfiguration The Kafka Streams configuration.
   * @return KafkaStreams The Kafka Streams instance.
   */
  @Bean
  public KafkaStreams kafkaStreams(
      @Qualifier("kafkaStreamsConfiguration") Properties streamConfiguration) {
    StreamsBuilder streamsBuilder = new StreamsBuilder();
    AsyncProcessTopology.buildTopology(streamsBuilder);
    LatestLogsConfiguration.lastRecords(streamsBuilder);
    var topology = streamsBuilder.build();
    var kafkaStreams = new KafkaStreams(topology, streamConfiguration);

    kafkaStreams.cleanUp();
    kafkaStreams.start();

    Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

    return kafkaStreams;
  }

  /**
   * This method is used to create the HostInfo object for the Kafka Streams application.
   *
   * @return HostInfo The HostInfo object.
   */
  @Bean
  public HostInfo hostInfo() {
    var split = kafkaStreamsHostInfo.split(":");
    return new HostInfo(split[0], Integer.parseInt(split[1]));
  }

  /**
   * This method is used to create the Kafka Receiver for consuming messages from Kafka.
   *
   * @return KafkaReceiver The Kafka Receiver.
   */
  @Bean
  public KafkaReceiver kafkaReceiver() {

    Map<String, Object> props = new HashMap<>();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "async-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        com.etendorx.asyncprocess.serdes.AsyncProcessDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    return new DefaultKafkaReceiver(ConsumerFactory.INSTANCE,
        ReceiverOptions.create(props).subscription(Collections.singleton(ASYNC_PROCESS)));
  }

  /**
   * This method is used to create the ProducerFactory for the Kafka Template for sending retries.
   *
   * @return ProducerFactory The ProducerFactory.
   */
  @Bean
  public ProducerFactory<String, String> sendRetryFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AsyncProcessSerializer.class);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * This method is used to create the Kafka Template for sending retries.
   *
   * @return KafkaTemplate The Kafka Template.
   */
  @Bean
  public KafkaTemplate<String, String> sendRetryTemplate() {
    return new KafkaTemplate<>(sendRetryFactory());
  }

}
