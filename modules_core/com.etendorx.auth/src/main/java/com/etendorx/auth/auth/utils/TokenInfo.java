package com.etendorx.auth.auth.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {
  private String id;
  private String validuntil;
  private String token;
  private String user;
  private String etrxOauthProvider;

  public String getId() {
    return id;
  }

  public String getValiduntil() {
    return validuntil;
  }

  public String getToken() {
    return token;
  }

  public String getUser() {
    return user;
  }

  public String getEtrxOauthProvider() {
    return etrxOauthProvider;
  }
}
