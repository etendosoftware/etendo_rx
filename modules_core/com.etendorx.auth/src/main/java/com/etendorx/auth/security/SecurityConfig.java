package com.etendorx.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;

import jakarta.servlet.http.HttpServletRequest;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  protected DefaultSecurityFilterChain configure(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository
  ) throws Exception {

    http
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/genToken").authenticated()
            .requestMatchers("/api/authenticate", "/error", "/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(Customizer.withDefaults());
    http.csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
