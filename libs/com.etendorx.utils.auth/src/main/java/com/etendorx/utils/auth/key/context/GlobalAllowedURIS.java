package com.etendorx.utils.auth.key.context;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * This class is used to allow uri's that are not protected by the authentication
 */
@Component
public class GlobalAllowedURIS implements AllowedURIS {
  /**
   * This array contains the uri's that are not protected by the authentication
   */
  private final String[] allowedURIS = {
      "/actuator/"
  };

  /**
   * This method is used to check if the uri is allowed
   * @param requestURI the uri to check
   * @return true if the uri is allowed, false otherwise
   */
  @Override
  public boolean isAllowed(String requestURI) {
    return Arrays.stream(allowedURIS).anyMatch(p -> StringUtils.startsWith(requestURI, p));
  }
}
