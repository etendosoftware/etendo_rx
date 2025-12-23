package com.etendorx.auth.auth.jwt;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class JwtRequest implements Serializable {
  private String username;
  private String password;
  private String service;
  private String secret;

  /**
   * default constructor for JSON Parsing
   */
  public JwtRequest() {
  }

  public JwtRequest(String username, String password, String service, String secret) {
    this.setUsername(username);
    this.setPassword(password);
    this.setService(service);
    this.setSecret(secret);
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    if (StringUtils.isBlank(secret)) {
      throw new IllegalArgumentException("Secret cannot be null or empty");
    }
    this.secret = secret;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    if (StringUtils.isBlank(service)) {
      throw new IllegalArgumentException("Service cannot be null or empty");
    }
    this.service = service;
  }
}
