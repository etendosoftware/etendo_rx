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

import com.etendorx.lib.kafka.topology.AsyncProcessTopology;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.state.HostInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Initial configuration of stream services
 */
@Configuration
public class StreamConfiguration {

  public static final String ASYNC_PROCESS_QUERIES = "async-process-queries";
  @Value("${kafka.streams.host.info:localhost:8080}")
  private String kafkaStreamsHostInfo;
  @Value("${kafka.streams.state.dir:/tmp/kafka-streams/async-process-queries}")
  private String kafkaStreamsStateDir;
  @Value("${bootstrap_server:localhost:9092}")
  private String bootstrapServer;

  @Bean
  public Properties kafkaStreamsConfiguration() {
    Properties properties = new Properties();
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, ASYNC_PROCESS_QUERIES);
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
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
  public HostInfo hostInfo() {
    var split = kafkaStreamsHostInfo.split(":");
    return new HostInfo(split[0], Integer.parseInt(split[1]));
  }
}
