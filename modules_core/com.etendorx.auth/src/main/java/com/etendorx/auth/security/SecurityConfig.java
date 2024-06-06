package com.etendorx.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;

import com.etendorx.auth.filter.ParameterExtractionFilter;

@RefreshScope
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Autowired
  private ParameterExtractionFilter parameterExtractionFilter;

  @Autowired
  private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

  @Bean
  protected DefaultSecurityFilterChain configure(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
    http
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/genToken").authenticated()
            .requestMatchers("/api/authenticate", "/error", "/actuator/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2Login(oauth2 -> oauth2
            .successHandler(customAuthenticationSuccessHandler)
        );
    http.addFilterBefore(parameterExtractionFilter, OAuth2AuthorizationRequestRedirectFilter.class);
    http.csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }
}
