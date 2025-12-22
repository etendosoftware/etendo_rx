package com.etendorx.asyncprocess.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SerdesExceptionTest {

  @Test
  void testConstructor_WithMessageAndCause() {
    String message = "Serialization failed";
    Throwable cause = new RuntimeException("Root cause");
    
    SerdesException exception = new SerdesException(message, cause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testConstructor_WithNullCause() {
    String message = "Deserialization error";
    
    SerdesException exception = new SerdesException(message, null);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertNull(exception.getCause(), "Cause should be null");
  }

  @Test
  void testConstructor_WithNullMessage() {
    Throwable cause = new IllegalArgumentException("Invalid data");
    
    SerdesException exception = new SerdesException(null, cause);
    
    assertNull(exception.getMessage(), "Message should be null");
    assertEquals(cause, exception.getCause(), "Cause should match");
  }

  @Test
  void testException_IsCheckedException() {
    SerdesException exception = new SerdesException("Test", null);
    
    assertTrue(exception instanceof Exception, "Should be an Exception");
    assertFalse(exception instanceof RuntimeException, "Should not be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(SerdesException.class, () -> {
      throw new SerdesException("Test exception", null);
    }, "Should be able to throw SerdesException");
  }

  @Test
  void testException_WithNestedCause() {
    Throwable rootCause = new NullPointerException("Root");
    Throwable intermediateCause = new IllegalStateException("Intermediate", rootCause);
    String message = "Serdes error";
    
    SerdesException exception = new SerdesException(message, intermediateCause);
    
    assertEquals(message, exception.getMessage(), "Message should match");
    assertEquals(intermediateCause, exception.getCause(), "Direct cause should match");
    assertEquals(rootCause, exception.getCause().getCause(), "Root cause should be preserved");
  }

  @Test
  void testException_MessageContainsCauseInfo() {
    Throwable cause = new RuntimeException("Underlying error");
    SerdesException exception = new SerdesException("Failed to serialize", cause);
    
    assertNotNull(exception.getMessage(), "Message should not be null");
    assertTrue(exception.getMessage().contains("serialize"), "Message should contain expected text");
  }

  @Test
  void testException_WithEmptyMessage() {
    String emptyMessage = "";
    Throwable cause = new Exception("Some cause");
    
    SerdesException exception = new SerdesException(emptyMessage, cause);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
    assertEquals(cause, exception.getCause(), "Cause should be preserved");
  }
}
