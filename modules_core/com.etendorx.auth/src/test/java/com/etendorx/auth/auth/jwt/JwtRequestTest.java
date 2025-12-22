package com.etendorx.auth.auth.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtRequestTest {

  @Test
  void testDefaultConstructor() {
    JwtRequest request = new JwtRequest();
    
    assertNotNull(request, "Request should be created");
    assertNull(request.getUsername(), "Username should be null by default");
    assertNull(request.getPassword(), "Password should be null by default");
    assertNull(request.getService(), "Service should be null by default");
    assertNull(request.getSecret(), "Secret should be null by default");
  }

  @Test
  void testParameterizedConstructor() {
    String username = "testUser";
    String password = "testPass";
    String service = "das";
    String secret = "testSecret";
    
    JwtRequest request = new JwtRequest(username, password, service, secret);
    
    assertEquals(username, request.getUsername(), "Username should match");
    assertEquals(password, request.getPassword(), "Password should match");
    assertEquals(service, request.getService(), "Service should match");
    assertEquals(secret, request.getSecret(), "Secret should match");
  }

  @Test
  void testSetUsername() {
    JwtRequest request = new JwtRequest();
    String username = "john.doe";
    
    request.setUsername(username);
    
    assertEquals(username, request.getUsername(), "Username should match");
  }

  @Test
  void testSetPassword() {
    JwtRequest request = new JwtRequest();
    String password = "securePassword123";
    
    request.setPassword(password);
    
    assertEquals(password, request.getPassword(), "Password should match");
  }

  @Test
  void testSetService_WithValidValue() {
    JwtRequest request = new JwtRequest();
    String service = "auth-service";
    
    request.setService(service);
    
    assertEquals(service, request.getService(), "Service should match");
  }

  @Test
  void testSetService_WithNullValue_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setService(null);
    }, "Should throw IllegalArgumentException for null service");
  }

  @Test
  void testSetService_WithEmptyString_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setService("");
    }, "Should throw IllegalArgumentException for empty service");
  }

  @Test
  void testSetSecret_WithValidValue() {
    JwtRequest request = new JwtRequest();
    String secret = "mySecret123";
    
    request.setSecret(secret);
    
    assertEquals(secret, request.getSecret(), "Secret should match");
  }

  @Test
  void testSetSecret_WithNullValue_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setSecret(null);
    }, "Should throw IllegalArgumentException for null secret");
  }

  @Test
  void testSetSecret_WithEmptyString_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setSecret("");
    }, "Should throw IllegalArgumentException for empty secret");
  }

  @Test
  void testIsSerializable() {
    JwtRequest request = new JwtRequest();
    
    assertTrue(request instanceof java.io.Serializable, "JwtRequest should be Serializable");
  }

  @Test
  void testSetUsername_WithNull() {
    JwtRequest request = new JwtRequest();
    
    request.setUsername(null);
    
    assertNull(request.getUsername(), "Username should be null");
  }

  @Test
  void testSetPassword_WithNull() {
    JwtRequest request = new JwtRequest();
    
    request.setPassword(null);
    
    assertNull(request.getPassword(), "Password should be null");
  }

  @Test
  void testSetService_WithWhitespace_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setService("   ");
    }, "Should throw IllegalArgumentException for whitespace-only service");
  }

  @Test
  void testSetSecret_WithWhitespace_ShouldThrowException() {
    JwtRequest request = new JwtRequest();
    
    assertThrows(IllegalArgumentException.class, () -> {
      request.setSecret("   ");
    }, "Should throw IllegalArgumentException for whitespace-only secret");
  }

  @Test
  void testAllFieldsCanBeSet() {
    JwtRequest request = new JwtRequest();
    
    request.setUsername("user1");
    request.setPassword("pass1");
    request.setService("service1");
    request.setSecret("secret1");
    
    assertEquals("user1", request.getUsername());
    assertEquals("pass1", request.getPassword());
    assertEquals("service1", request.getService());
    assertEquals("secret1", request.getSecret());
  }

  @Test
  void testParameterizedConstructor_WithSpecialCharacters() {
    String username = "user@example.com";
    String password = "p@$$w0rd!";
    String service = "auth-service-v2";
    String secret = "secret#123";
    
    JwtRequest request = new JwtRequest(username, password, service, secret);
    
    assertEquals(username, request.getUsername());
    assertEquals(password, request.getPassword());
    assertEquals(service, request.getService());
    assertEquals(secret, request.getSecret());
  }
}
