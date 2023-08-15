package com.etendorx.utils.auth.key.exceptions;

public class JwtKeyException extends RuntimeException {
  public JwtKeyException(String message, Throwable e) {
    super(message, e);
  }
}
