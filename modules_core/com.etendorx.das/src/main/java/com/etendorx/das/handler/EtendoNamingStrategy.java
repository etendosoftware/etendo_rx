package com.etendorx.das.handler;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtendoNamingStrategy {
  @Bean
  public PhysicalNamingStrategy physical() {
    return new PhysicalNamingStrategyStandardImpl();
  }

  @Bean
  public ImplicitNamingStrategy implicit() {
    return new ImplicitNamingStrategyLegacyJpaImpl();
  }
}
