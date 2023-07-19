package com.etendorx.das.test.eventhandlertest.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.mock.mockito.SpyBeans;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.etendorx.das.test.eventhandlertest.repository.ParentEntityRepository;

@Configuration
@EnableJpaAuditing

public class Config {
  @Autowired
  private ParentEntityRepository parentEntityRepository;
}
