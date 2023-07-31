package com.etendorx.das.mime;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/*
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new MyClassHttpMessageConverter());
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new CustomArgumentResolver(new MyClassHttpMessageConverter(), new MyClassHttpMessageConverter()));
  }
}
*/
