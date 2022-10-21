package com.etendorx.auth.auth.jwt;

import java.io.Serializable;

public class JwtResponse implements Serializable {

  private String token;

  /**
   * default constructor for JSON Parsing
   */
  public JwtResponse() {
  }

  public JwtResponse(String token) {
    this.token = token;
  }

  public String getToken() {
    return this.token;
  }
}
