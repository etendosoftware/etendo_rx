package com.etendorx.utils.auth.key.context;

public interface AllowedURIS {
  boolean isAllowed(String requestURI);
}
