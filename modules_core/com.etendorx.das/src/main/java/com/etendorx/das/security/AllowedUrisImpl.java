package com.etendorx.das.security;

import com.etendorx.utils.auth.key.context.AllowedURIS;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class AllowedUrisImpl implements AllowedURIS
{
  @Override
  public boolean isAllowed(String requestURI) {
    if(StringUtils.startsWith(requestURI, "/v3/api-docs")) {
      return true;
    }
    if(StringUtils.endsWith(requestURI, ".png")) {
      return true;
    }
    if(StringUtils.endsWith(requestURI, ".ico")) {
      return true;
    }
    return false;
  }
}
