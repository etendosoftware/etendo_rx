/*
 * Copyright 2023  Futit Services SL
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
package com.etendorx.clientrest.base;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for RestTemplate with HAL support.
 */
@Component
public class RestTemplateFactory implements FactoryBean<RestTemplate> {

  private final TokenRequestInterceptor tokenRequestInterceptor;
  private RestTemplate restTemplate;

  @Autowired
  public RestTemplateFactory(TokenRequestInterceptor tokenRequestInterceptor) {
    this.tokenRequestInterceptor = tokenRequestInterceptor;
  }

  @Override
  public RestTemplate getObject() {
    return restTemplate;
  }

  @Override
  public Class<?> getObjectType() {
    return RestTemplate.class;
  }

  /**
   * Initialize RestTemplate with HAL support
   */
  @PostConstruct
  public void init() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new Jackson2HalModule());
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(MediaType.parseMediaTypes("application/hal+json"));
    converter.setObjectMapper(mapper);
    restTemplate = new RestTemplate(List.of(converter));
    restTemplate.getInterceptors().add(tokenRequestInterceptor);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
