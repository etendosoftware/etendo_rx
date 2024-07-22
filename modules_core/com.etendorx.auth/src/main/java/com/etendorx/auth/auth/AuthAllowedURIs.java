package com.etendorx.auth.auth;

import com.etendorx.utils.auth.key.context.AllowedURIS;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AuthAllowedURIs implements AllowedURIS {
  public static final String AUTH_PATH_URI = "/api/authenticate"; //NOSONAR

  @Override
  public boolean isAllowed(String requestURI) {
    return StringUtils.startsWith(requestURI, AUTH_PATH_URI);
  }
}
