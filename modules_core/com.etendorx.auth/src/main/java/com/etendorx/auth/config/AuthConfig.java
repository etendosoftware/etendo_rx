package com.etendorx.auth.config;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AuthConfig {
  @Value("${token}")
  private String token;

  @Value("${private-key}")
  private String privateKey;

  public String getToken() {
    return token;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  @Bean
  public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
    return userRequest -> {
      Set<GrantedAuthority> authorities = new LinkedHashSet<>();
      Map<String, Object> userAttributes = new HashMap<>();
      userAttributes.put("name", "test");
      userAttributes.put("token", userRequest.getAccessToken().getTokenValue());
      userAttributes.put("scopes", userRequest.getAccessToken().getScopes().stream().map(s -> s.toString()).toArray());
      userAttributes.put("expiresAt", userRequest.getAccessToken().getExpiresAt());
      userAttributes.put("issuedAt", userRequest.getAccessToken().getIssuedAt());
      String userNameAttributeName = "name";
      return new DefaultOAuth2User(authorities, userAttributes, userNameAttributeName);
    };
  }
}

