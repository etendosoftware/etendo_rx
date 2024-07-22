package com.etendorx.auth.config;

import org.springframework.beans.factory.annotation.Value;
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
}

