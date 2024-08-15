package com.etendorx.utils.auth.key.context;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * This class is used to allow uri's that are not protected by the authentication
 */
@Component
public class GlobalAllowedURIS implements AllowedURIS {
  /**
   * This array contains the uri's that are not protected by the authentication
   */
  private final String[] startsWith = { "/actuator/", "/v3/api-docs", "/api-docs", "/swagger-ui" };
  private final String[] endsWith = { ".png", ".ico" };

  /**
   * This method is used to check if the uri is allowed
   *
   * @param requestURI the uri to check
   * @return true if the uri is allowed, false otherwise
   */
  @Override
  public boolean isAllowed(String requestURI) {
    if (Arrays.stream(startsWith).anyMatch(p -> StringUtils.startsWith(requestURI, p))) {
      return true;
    }
    return Arrays.stream(endsWith).anyMatch(p -> StringUtils.endsWith(requestURI, p));
  }
}
