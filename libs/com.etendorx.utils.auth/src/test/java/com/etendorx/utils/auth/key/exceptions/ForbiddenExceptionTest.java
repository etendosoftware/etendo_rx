package com.etendorx.utils.auth.key.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import static org.junit.jupiter.api.Assertions.*;

class ForbiddenExceptionTest {

  @Test
  void testDefaultConstructor() {
    ForbiddenException exception = new ForbiddenException();
    
    assertNotNull(exception, "Exception should be created");
    assertNull(exception.getMessage(), "Default message should be null");
  }

  @Test
  void testConstructor_WithMessage() {
    String message = "Access denied";
    
    ForbiddenException exception = new ForbiddenException(message);
    
    assertEquals(message, exception.getMessage(), "Message should match");
  }

  @Test
  void testConstructor_WithNullMessage() {
    ForbiddenException exception = new ForbiddenException(null);
    
    assertNull(exception.getMessage(), "Message should be null");
  }

  @Test
  void testException_IsRuntimeException() {
    ForbiddenException exception = new ForbiddenException();
    
    assertTrue(exception instanceof RuntimeException, "Should be a RuntimeException");
  }

  @Test
  void testException_CanBeThrown() {
    assertThrows(ForbiddenException.class, () -> {
      throw new ForbiddenException("Not authorized");
    }, "Should be able to throw ForbiddenException");
  }

  @Test
  void testException_HasResponseStatusAnnotation() {
    ResponseStatus annotation = ForbiddenException.class.getAnnotation(ResponseStatus.class);
    
    assertNotNull(annotation, "Should have @ResponseStatus annotation");
    assertEquals(HttpStatus.FORBIDDEN, annotation.value(), 
        "Should have HttpStatus.FORBIDDEN status");
  }

  @Test
  void testException_WithDetailedMessage() {
    String detailedMessage = "User does not have permission to access this resource";
    
    ForbiddenException exception = new ForbiddenException(detailedMessage);
    
    assertEquals(detailedMessage, exception.getMessage(), "Detailed message should be preserved");
  }

  @Test
  void testException_WithEmptyMessage() {
    String emptyMessage = "";
    
    ForbiddenException exception = new ForbiddenException(emptyMessage);
    
    assertEquals(emptyMessage, exception.getMessage(), "Empty message should be preserved");
  }

  @Test
  void testException_CanBeCaught() {
    try {
      throw new ForbiddenException("Test");
    } catch (ForbiddenException e) {
      assertEquals("Test", e.getMessage(), "Should catch and access message");
    }
  }

  @Test
  void testException_CanBeCaughtAsRuntimeException() {
    try {
      throw new ForbiddenException("Test");
    } catch (RuntimeException e) {
      assertTrue(e instanceof ForbiddenException, "Should be catchable as RuntimeException");
      assertEquals("Test", e.getMessage(), "Message should be accessible");
    }
  }
}
