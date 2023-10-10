package com.etendorx.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  protected DefaultSecurityFilterChain configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((authz) -> authz.anyRequest().permitAll());
    http.csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

}
