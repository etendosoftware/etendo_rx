package com.etendorx.das.externalid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalIdExceptionTest {

  @Test
  void testConstructor_WithMessage() {
    String message = "External ID not found";
    
    ExternalIdException exception = new ExternalIdException(message);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null when not provided");
  }

  @Test
  void testConstructor_WithMessageAndCause() {
    String message = "Failed to generate external ID";
    Throwable cause = new IllegalStateException("Invalid state");
    
    ExternalIdException exception = new ExternalIdException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testConstructor_WithNullMessage() {
    ExternalIdException exception = new ExternalIdException(null);
    
    assertNull(exception.getMessage(), "Message should be null");
  }

  @Test
  void testConstructor_WithNullCause() {
    String message = "External ID error";
    
    ExternalIdException exception = new ExternalIdException(message, null);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null");
  }

  @Test
  void testException_IsRuntimeException() {
    ExternalIdException exception = new ExternalIdException("Test");
    
    assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(ExternalIdException.class, () -> {
      throw new ExternalIdException("Test exception");
    }, "Should be able to throw ExternalIdException");
  }

  @Test
  void testException_CanBeThrownWithCause() {
    Throwable cause = new RuntimeException("Root cause");
    
    assertThrows(ExternalIdException.class, () -> {
      throw new ExternalIdException("External ID error", cause);
    }, "Should be able to throw ExternalIdException with cause");
  }

  @Test
  void testException_WithNestedCause() {
    Throwable rootCause = new NullPointerException("Null entity");
    Throwable intermediateCause = new IllegalArgumentException("Invalid argument", rootCause);
    String message = "External ID validation failed";
    
    ExternalIdException exception = new ExternalIdException(message, intermediateCause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(intermediateCause, exception.getCause(), "Direct cause should match");
    assertEquals(rootCause, exception.getCause().getCause(), "Root cause should be preserved");
  }

  @Test
  void testException_WithDetailedMessage() {
    String detailedMessage = "External ID 'ext-123' already exists for entity type 'Product'";
    
    ExternalIdException exception = new ExternalIdException(detailedMessage);
    
    assertEquals(detailedMessage, exception.getMessage(), "Detailed message should be preserved");
    assertTrue(exception.getMessage().contains("ext-123"), "Message should contain ID");
    assertTrue(exception.getMessage().contains("Product"), "Message should contain entity type");
  }

  @Test
  void testException_WithEmptyMessage() {
    String emptyMessage = "";
    
    ExternalIdException exception = new ExternalIdException(emptyMessage);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
  }

  @Test
  void testException_CanBeCaughtAsRuntimeException() {
    try {
      throw new ExternalIdException("Test");
    } catch (RuntimeException e) {
      assertTrue(e instanceof ExternalIdException, "Should be catchable as RuntimeException");
      assertEquals("Test", e.getMessage(), "Message should be accessible");
    }
  }

  @Test
  void testException_WithDatabaseError() {
    String message = "Database constraint violation on external ID";
    Throwable cause = new java.sql.SQLException("Unique constraint violated");
    
    ExternalIdException exception = new ExternalIdException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertTrue(exception.getCause() instanceof java.sql.SQLException, 
        "Cause should be SQLException");
  }

  @Test
  void testException_WithLongMessage() {
    String longMessage = "External ID error: " + "x".repeat(500);
    
    ExternalIdException exception = new ExternalIdException(longMessage);
    
    assertEquals(longMessage, exception.getMessage(), "Long message should be preserved");
    assertTrue(exception.getMessage().length() > 500, "Message length should be preserved");
  }

  @Test
  void testException_WithSpecialCharacters() {
    String messageWithSpecialChars = "External ID contains invalid characters: @#$%";
    
    ExternalIdException exception = new ExternalIdException(messageWithSpecialChars);
    
    assertEquals(messageWithSpecialChars, exception.getMessage(), 
        "Special characters should be preserved");
  }
}
