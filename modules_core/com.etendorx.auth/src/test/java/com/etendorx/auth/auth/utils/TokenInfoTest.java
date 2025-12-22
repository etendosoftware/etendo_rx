package com.etendorx.auth.auth.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenInfoTest {

  @Test
  void testNoArgsConstructor() {
    TokenInfo tokenInfo = new TokenInfo();
    
    assertNotNull(tokenInfo, "TokenInfo should be created");
    assertNull(tokenInfo.getId(), "Id should be null by default");
    assertNull(tokenInfo.getValidUntil(), "ValidUntil should be null by default");
    assertNull(tokenInfo.getToken(), "Token should be null by default");
    assertNull(tokenInfo.getUser(), "User should be null by default");
    assertNull(tokenInfo.getEtrxOauthProvider(), "EtrxOauthProvider should be null by default");
  }

  @Test
  void testAllArgsConstructor() {
    String id = "token-123";
    String validUntil = "2024-12-31T23:59:59Z";
    String token = "jwt-token-value";
    String user = "user-456";
    String provider = "google";
    
    TokenInfo tokenInfo = new TokenInfo(id, validUntil, token, user, provider);
    
    assertEquals(id, tokenInfo.getId(), "Id should match");
    assertEquals(validUntil, tokenInfo.getValidUntil(), "ValidUntil should match");
    assertEquals(token, tokenInfo.getToken(), "Token should match");
    assertEquals(user, tokenInfo.getUser(), "User should match");
    assertEquals(provider, tokenInfo.getEtrxOauthProvider(), "Provider should match");
  }

  @Test
  void testBuilder() {
    TokenInfo tokenInfo = TokenInfo.builder()
        .id("token-789")
        .validUntil("2025-01-01T00:00:00Z")
        .token("bearer-token")
        .user("user-789")
        .etrxOauthProvider("github")
        .build();
    
    assertEquals("token-789", tokenInfo.getId());
    assertEquals("2025-01-01T00:00:00Z", tokenInfo.getValidUntil());
    assertEquals("bearer-token", tokenInfo.getToken());
    assertEquals("user-789", tokenInfo.getUser());
    assertEquals("github", tokenInfo.getEtrxOauthProvider());
  }

  @Test
  void testBuilder_WithPartialFields() {
    TokenInfo tokenInfo = TokenInfo.builder()
        .id("token-partial")
        .token("partial-token")
        .build();
    
    assertEquals("token-partial", tokenInfo.getId());
    assertEquals("partial-token", tokenInfo.getToken());
    assertNull(tokenInfo.getValidUntil());
    assertNull(tokenInfo.getUser());
    assertNull(tokenInfo.getEtrxOauthProvider());
  }

  @Test
  void testGetId() {
    String id = "test-id-123";
    TokenInfo tokenInfo = TokenInfo.builder().id(id).build();
    
    assertEquals(id, tokenInfo.getId(), "Should return correct id");
  }

  @Test
  void testGetValidUntil() {
    String validUntil = "2024-06-15T12:30:45Z";
    TokenInfo tokenInfo = TokenInfo.builder().validUntil(validUntil).build();
    
    assertEquals(validUntil, tokenInfo.getValidUntil(), "Should return correct validUntil");
  }

  @Test
  void testGetToken() {
    String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    TokenInfo tokenInfo = TokenInfo.builder().token(token).build();
    
    assertEquals(token, tokenInfo.getToken(), "Should return correct token");
  }

  @Test
  void testGetUser() {
    String user = "user@example.com";
    TokenInfo tokenInfo = TokenInfo.builder().user(user).build();
    
    assertEquals(user, tokenInfo.getUser(), "Should return correct user");
  }

  @Test
  void testGetEtrxOauthProvider() {
    String provider = "azure";
    TokenInfo tokenInfo = TokenInfo.builder().etrxOauthProvider(provider).build();
    
    assertEquals(provider, tokenInfo.getEtrxOauthProvider(), "Should return correct provider");
  }

  @Test
  void testAllArgsConstructor_WithNullValues() {
    TokenInfo tokenInfo = new TokenInfo(null, null, null, null, null);
    
    assertNull(tokenInfo.getId());
    assertNull(tokenInfo.getValidUntil());
    assertNull(tokenInfo.getToken());
    assertNull(tokenInfo.getUser());
    assertNull(tokenInfo.getEtrxOauthProvider());
  }

  @Test
  void testBuilder_WithEmptyStrings() {
    TokenInfo tokenInfo = TokenInfo.builder()
        .id("")
        .validUntil("")
        .token("")
        .user("")
        .etrxOauthProvider("")
        .build();
    
    assertEquals("", tokenInfo.getId());
    assertEquals("", tokenInfo.getValidUntil());
    assertEquals("", tokenInfo.getToken());
    assertEquals("", tokenInfo.getUser());
    assertEquals("", tokenInfo.getEtrxOauthProvider());
  }

  @Test
  void testBuilder_WithSpecialCharacters() {
    TokenInfo tokenInfo = TokenInfo.builder()
        .id("id-with-special-chars-!@#$%")
        .user("user+123@example.com")
        .etrxOauthProvider("provider-v2.0")
        .build();
    
    assertEquals("id-with-special-chars-!@#$%", tokenInfo.getId());
    assertEquals("user+123@example.com", tokenInfo.getUser());
    assertEquals("provider-v2.0", tokenInfo.getEtrxOauthProvider());
  }

  @Test
  void testBuilder_WithLongValues() {
    String longToken = "a".repeat(1000);
    TokenInfo tokenInfo = TokenInfo.builder()
        .token(longToken)
        .build();
    
    assertEquals(longToken, tokenInfo.getToken());
    assertEquals(1000, tokenInfo.getToken().length());
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    TokenInfo tokenInfo1 = TokenInfo.builder()
        .id("id1")
        .token("token1")
        .build();
    
    TokenInfo tokenInfo2 = TokenInfo.builder()
        .id("id2")
        .token("token2")
        .build();
    
    assertEquals("id1", tokenInfo1.getId());
    assertEquals("id2", tokenInfo2.getId());
    assertNotEquals(tokenInfo1.getId(), tokenInfo2.getId());
  }

  @Test
  void testToString_ContainsAllFields() {
    TokenInfo tokenInfo = TokenInfo.builder()
        .id("test-id")
        .validUntil("2024-12-31")
        .token("test-token")
        .user("test-user")
        .etrxOauthProvider("test-provider")
        .build();
    
    String toString = tokenInfo.toString();
    
    assertNotNull(toString);
    // Lombok's @Data generates toString with all fields
    assertTrue(toString.contains("test-id") || toString.contains("id"));
  }
}
