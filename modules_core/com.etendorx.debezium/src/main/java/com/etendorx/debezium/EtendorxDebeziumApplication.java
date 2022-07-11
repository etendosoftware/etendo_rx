package com.etendorx.debezium;

import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.fn.common.cdc.CdcAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
@AutoConfigureBefore(CdcAutoConfiguration.class)
public class EtendorxDebeziumApplication {
  public static void main(String[] args) {
    SpringApplication.run(EtendorxDebeziumApplication.class, args);
  }

  @Bean
  public Function<SourceRecord, String> mySourceRecordConsumer() {
    return sourceRecord -> {
      System.out.println(" My handler: " + sourceRecord.toString());
      return sourceRecord.toString();
    };
  }
}
