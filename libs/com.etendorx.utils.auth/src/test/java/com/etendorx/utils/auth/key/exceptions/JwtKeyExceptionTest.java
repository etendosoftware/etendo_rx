package com.etendorx.utils.auth.key.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtKeyExceptionTest {

  @Test
  void testConstructor_WithMessageAndCause() {
    String message = "JWT key validation failed";
    Throwable cause = new IllegalArgumentException("Invalid key format");
    
    JwtKeyException exception = new JwtKeyException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testConstructor_WithNullMessage() {
    Throwable cause = new RuntimeException("Root cause");
    
    JwtKeyException exception = new JwtKeyException(null, cause);
    
    assertNull(exception.getMessage(), "Message should be null");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testConstructor_WithNullCause() {
    String message = "JWT error";
    
    JwtKeyException exception = new JwtKeyException(message, null);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null");
  }

  @Test
  void testException_IsRuntimeException() {
    JwtKeyException exception = new JwtKeyException("Test", null);
    
    assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(JwtKeyException.class, () -> {
      throw new JwtKeyException("Test exception", new Exception("cause"));
    }, "Should be able to throw JwtKeyException");
  }

  @Test
  void testException_WithNestedCause() {
    Throwable rootCause = new NullPointerException("Null key");
    Throwable intermediateCause = new IllegalStateException("Invalid state", rootCause);
    String message = "JWT key processing failed";
    
    JwtKeyException exception = new JwtKeyException(message, intermediateCause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(intermediateCause, exception.getCause(), "Direct cause should match");
    assertEquals(rootCause, exception.getCause().getCause(), "Root cause should be preserved");
  }

  @Test
  void testException_WithDetailedMessage() {
    String detailedMessage = "Failed to load JWT public key from file: /path/to/key.pem";
    Throwable cause = new java.io.IOException("File not found");
    
    JwtKeyException exception = new JwtKeyException(detailedMessage, cause);
    
    assertEquals(detailedMessage, exception.getMessage(), "Detailed message should be preserved");
    assertTrue(exception.getCause() instanceof java.io.IOException, "Cause should be IOException");
  }

  @Test
  void testException_WithEmptyMessage() {
    String emptyMessage = "";
    Throwable cause = new Exception("Some cause");
    
    JwtKeyException exception = new JwtKeyException(emptyMessage, cause);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
    assertEquals(cause, exception.getCause(), "Cause should be preserved");
  }

  @Test
  void testException_MessageContainsKeyInfo() {
    String message = "JWT key error: algorithm mismatch";
    Throwable cause = new Exception("Expected RS256 but got HS256");
    
    JwtKeyException exception = new JwtKeyException(message, cause);
    
    assertNotNull(exception.getMessage(), "Message should not be null");
    assertTrue(exception.getMessage().contains("key"), "Message should contain 'key'");
  }

  @Test
  void testException_CanBeCaughtAsRuntimeException() {
    try {
      throw new JwtKeyException("Test", new Exception());
    } catch (RuntimeException e) {
      assertTrue(e instanceof JwtKeyException, "Should be catchable as RuntimeException");
    }
  }
}
