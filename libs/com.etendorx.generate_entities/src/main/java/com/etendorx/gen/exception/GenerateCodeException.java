package com.etendorx.gen.exception;

/**
 * Exception thrown when an error occurs while generating code
 */
public class GenerateCodeException extends RuntimeException {
  public GenerateCodeException(String message) {
    super(message);
  }
}
