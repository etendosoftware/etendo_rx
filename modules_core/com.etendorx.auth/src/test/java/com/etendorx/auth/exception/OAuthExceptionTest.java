package com.etendorx.auth.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OAuthExceptionTest {

  @Test
  void testConstructor_WithMessage() {
    String message = "OAuth authentication failed";
    
    OAuthException exception = new OAuthException(message);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null when not provided");
  }

  @Test
  void testConstructor_WithMessageAndCause() {
    String message = "Token decode error";
    Throwable cause = new IllegalArgumentException("Invalid token format");
    
    OAuthException exception = new OAuthException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testConstructor_WithNullMessage() {
    OAuthException exception = new OAuthException(null);
    
    assertNull(exception.getMessage(), "Message should be null");
  }

  @Test
  void testConstructor_WithNullCause() {
    String message = "OAuth flow failed";
    
    OAuthException exception = new OAuthException(message, null);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null");
  }

  @Test
  void testException_IsRuntimeException() {
    OAuthException exception = new OAuthException("Test");
    
    assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(OAuthException.class, () -> {
      throw new OAuthException("Test exception");
    }, "Should be able to throw OAuthException");
  }

  @Test
  void testException_CanBeThrownWithCause() {
    Throwable cause = new RuntimeException("Root cause");
    
    assertThrows(OAuthException.class, () -> {
      throw new OAuthException("OAuth error", cause);
    }, "Should be able to throw OAuthException with cause");
  }

  @Test
  void testException_WithNestedCause() {
    Throwable rootCause = new NullPointerException("Missing state parameter");
    Throwable intermediateCause = new IllegalStateException("Invalid state", rootCause);
    String message = "OAuth state validation failed";
    
    OAuthException exception = new OAuthException(message, intermediateCause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(intermediateCause, exception.getCause(), "Direct cause should match");
    assertEquals(rootCause, exception.getCause().getCause(), "Root cause should be preserved");
  }

  @Test
  void testException_WithDetailedMessage() {
    String detailedMessage = "Invalid state parameter: expected 'abc123' but got 'xyz789'";
    
    OAuthException exception = new OAuthException(detailedMessage);
    
    assertEquals(detailedMessage, exception.getMessage(), "Detailed message should be preserved");
    assertTrue(exception.getMessage().contains("abc123"), "Message should contain details");
  }

  @Test
  void testException_WithURIParsingError() {
    String message = "Failed to parse OAuth redirect URI";
    Throwable cause = new java.net.URISyntaxException("invalid://uri", "Invalid scheme");
    
    OAuthException exception = new OAuthException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertTrue(exception.getCause() instanceof java.net.URISyntaxException, 
        "Cause should be URISyntaxException");
  }

  @Test
  void testException_MessageShouldBeDescriptive() {
    String message = "OAuth error occurred during authentication";
    OAuthException exception = new OAuthException(message);
    
    assertNotNull(exception.getMessage(), "Message should not be null");
    assertFalse(exception.getMessage().isEmpty(), "Message should not be empty");
  }

  @Test
  void testException_WithEmptyMessage() {
    String emptyMessage = "";
    
    OAuthException exception = new OAuthException(emptyMessage);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
  }
}
