package com.etendorx.auth.auth.jwt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtResponseTest {

  @Test
  void testDefaultConstructor() {
    JwtResponse response = new JwtResponse();
    
    assertNotNull(response, "Response should be created");
    assertNull(response.getToken(), "Token should be null by default");
  }

  @Test
  void testParameterizedConstructor() {
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    
    JwtResponse response = new JwtResponse(token);
    
    assertEquals(token, response.getToken(), "Token should match");
  }

  @Test
  void testParameterizedConstructor_WithNullToken() {
    JwtResponse response = new JwtResponse(null);
    
    assertNull(response.getToken(), "Token should be null");
  }

  @Test
  void testGetToken_ReturnsCorrectValue() {
    String token = "test.jwt.token";
    JwtResponse response = new JwtResponse(token);
    
    assertEquals(token, response.getToken(), "Should return the token");
  }

  @Test
  void testIsSerializable() {
    JwtResponse response = new JwtResponse();
    
    assertTrue(response instanceof java.io.Serializable, "JwtResponse should be Serializable");
  }

  @Test
  void testToken_WithLongValue() {
    StringBuilder longToken = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longToken.append("a");
    }
    String token = longToken.toString();
    
    JwtResponse response = new JwtResponse(token);
    
    assertEquals(token, response.getToken(), "Should handle long token");
    assertEquals(1000, response.getToken().length(), "Token length should be preserved");
  }

  @Test
  void testToken_WithEmptyString() {
    String emptyToken = "";
    
    JwtResponse response = new JwtResponse(emptyToken);
    
    assertEquals(emptyToken, response.getToken(), "Should accept empty string");
  }

  @Test
  void testToken_WithSpecialCharacters() {
    String tokenWithSpecialChars = "token.with-special_chars+123/456=";
    
    JwtResponse response = new JwtResponse(tokenWithSpecialChars);
    
    assertEquals(tokenWithSpecialChars, response.getToken(), "Should preserve special characters");
  }

  @Test
  void testToken_WithUnicodeCharacters() {
    String unicodeToken = "token-密码-🔐";
    
    JwtResponse response = new JwtResponse(unicodeToken);
    
    assertEquals(unicodeToken, response.getToken(), "Should handle unicode characters");
  }

  @Test
  void testToken_WithWhitespace() {
    String tokenWithWhitespace = "token with spaces";
    
    JwtResponse response = new JwtResponse(tokenWithWhitespace);
    
    assertEquals(tokenWithWhitespace, response.getToken(), "Should preserve whitespace");
  }

  @Test
  void testDefaultConstructor_ThenSetViaReflection() throws Exception {
    JwtResponse response = new JwtResponse();
    String token = "new.token.value";
    
    // Using reflection to set the token (simulating JSON deserialization)
    java.lang.reflect.Field tokenField = JwtResponse.class.getDeclaredField("token");
    tokenField.setAccessible(true);
    tokenField.set(response, token);
    
    assertEquals(token, response.getToken(), "Token should be set correctly");
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    String token1 = "token1";
    String token2 = "token2";
    
    JwtResponse response1 = new JwtResponse(token1);
    JwtResponse response2 = new JwtResponse(token2);
    
    assertEquals(token1, response1.getToken(), "First response should have token1");
    assertEquals(token2, response2.getToken(), "Second response should have token2");
    assertNotEquals(response1.getToken(), response2.getToken(), "Tokens should be different");
  }

  @Test
  void testToken_WithRealJwtFormat() {
    // A sample JWT token format (not a valid signature, just for testing format)
    String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                      "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                      "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    
    JwtResponse response = new JwtResponse(jwtToken);
    
    assertEquals(jwtToken, response.getToken(), "Should handle JWT format");
    assertTrue(response.getToken().split("\\.").length == 3, "JWT should have three parts");
  }
}
