package com.etendorx.clientrest.base;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RestUtilsExceptionTest {

  @Test
  void testConstructor_WithMessage() {
    String message = "REST call failed";
    
    RestUtilsException exception = new RestUtilsException(message);
    
    assertEquals(message, exception.getMessage(), "Message should match");
  }

  @Test
  void testConstructor_WithNullMessage() {
    RestUtilsException exception = new RestUtilsException(null);
    
    assertNull(exception.getMessage(), "Message should be null");
  }

  @Test
  void testConstructor_WithEmptyMessage() {
    String emptyMessage = "";
    
    RestUtilsException exception = new RestUtilsException(emptyMessage);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
  }

  @Test
  void testException_IsRuntimeException() {
    RestUtilsException exception = new RestUtilsException("Test");
    
    assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(RestUtilsException.class, () -> {
      throw new RestUtilsException("Test exception");
    }, "Should be able to throw RestUtilsException");
  }

  @Test
  void testException_WithDetailedMessage() {
    String detailedMessage = "Failed to connect to REST endpoint: http://example.com/api";
    
    RestUtilsException exception = new RestUtilsException(detailedMessage);
    
    assertEquals(detailedMessage, exception.getMessage(), "Detailed message should be preserved");
    assertTrue(exception.getMessage().contains("http://"), "Message should contain URL");
  }

  @Test
  void testException_CanBeCaughtAsRuntimeException() {
    try {
      throw new RestUtilsException("Test");
    } catch (RuntimeException e) {
      assertTrue(e instanceof RestUtilsException, "Should be catchable as RuntimeException");
      assertEquals("Test", e.getMessage(), "Message should be accessible");
    }
  }

  @Test
  void testException_CanBeCaughtAsException() {
    try {
      throw new RestUtilsException("Test");
    } catch (Exception e) {
      assertTrue(e instanceof RestUtilsException, "Should be catchable as Exception");
    }
  }

  @Test
  void testException_WithLongMessage() {
    String longMessage = "Error occurred: " + "x".repeat(1000);
    
    RestUtilsException exception = new RestUtilsException(longMessage);
    
    assertEquals(longMessage, exception.getMessage(), "Long message should be preserved");
    assertTrue(exception.getMessage().length() > 1000, "Message length should be preserved");
  }

  @Test
  void testException_WithSpecialCharacters() {
    String messageWithSpecialChars = "REST error: Invalid characters !@#$%^&*()";
    
    RestUtilsException exception = new RestUtilsException(messageWithSpecialChars);
    
    assertEquals(messageWithSpecialChars, exception.getMessage(), "Special characters should be preserved");
  }

  @Test
  void testException_WithUnicodeCharacters() {
    String unicodeMessage = "Error: 错误 произошла ошибка";
    
    RestUtilsException exception = new RestUtilsException(unicodeMessage);
    
    assertEquals(unicodeMessage, exception.getMessage(), "Unicode should be preserved");
  }

  @Test
  void testException_WithJsonMessage() {
    String jsonMessage = "{\"error\": \"Connection refused\", \"code\": 500}";
    
    RestUtilsException exception = new RestUtilsException(jsonMessage);
    
    assertEquals(jsonMessage, exception.getMessage(), "JSON message should be preserved");
    assertTrue(exception.getMessage().contains("error"), "JSON content should be intact");
  }

  @Test
  void testException_MessageIsReadable() {
    String message = "Unable to parse REST response";
    RestUtilsException exception = new RestUtilsException(message);
    
    assertNotNull(exception.getMessage(), "Message should not be null");
    assertFalse(exception.getMessage().isEmpty(), "Message should not be empty");
  }
}
