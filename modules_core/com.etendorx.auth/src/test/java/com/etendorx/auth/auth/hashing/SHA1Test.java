package com.etendorx.auth.auth.hashing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SHA1Test {

  private final SHA1 sha1 = new SHA1();

  @Test
  void testGenerateHash_ShouldReturnNonNull() {
    String password = "testPassword";
    String hash = sha1.generateHash(password);
    
    assertNotNull(hash, "Hash should not be null");
    assertFalse(hash.isEmpty(), "Hash should not be empty");
  }

  @Test
  void testGenerateHash_ShouldReturnSameHashForSamePassword() {
    String password = "samePassword";
    String hash1 = sha1.generateHash(password);
    String hash2 = sha1.generateHash(password);
    
    assertEquals(hash1, hash2, "SHA-1 should produce same hash for same password (no salt)");
  }

  @Test
  void testGenerateHash_ShouldNotContainDollarSign() {
    String password = "password";
    String hash = sha1.generateHash(password);
    
    assertFalse(hash.contains("$"), "SHA-1 hash should not contain version/salt separators");
  }

  @Test
  void testCheck_ShouldReturnTrueForMatchingPassword() {
    String password = "correctPassword";
    String hash = sha1.generateHash(password);
    
    assertTrue(sha1.check(password, hash), "Password should match its own hash");
  }

  @Test
  void testCheck_ShouldReturnFalseForWrongPassword() {
    String password = "correctPassword";
    String wrongPassword = "wrongPassword";
    String hash = sha1.generateHash(password);
    
    assertFalse(sha1.check(wrongPassword, hash), "Wrong password should not match hash");
  }

  @Test
  void testCheck_ShouldReturnFalseForNullPassword() {
    String hash = sha1.generateHash("somePassword");
    
    assertFalse(sha1.check(null, hash), "Null password should not match any hash");
  }

  @Test
  void testCheck_ShouldReturnFalseForNullHash() {
    String password = "password";
    
    assertFalse(sha1.check(password, null), "Password should not match null hash");
  }

  @Test
  void testGenerateHash_WithNullPassword_ShouldReturnNull() {
    String hash = sha1.generateHash(null);
    
    assertNull(hash, "Hash of null password should be null");
  }

  @Test
  void testGetAlgorithmVersion_ShouldReturnZero() {
    assertEquals(0, sha1.getAlgorithmVersion(), "SHA-1 algorithm version should be 0");
  }

  @Test
  void testGenerateHash_WithEmptyPassword() {
    String emptyPassword = "";
    String hash = sha1.generateHash(emptyPassword);
    
    assertNotNull(hash, "Hash of empty password should not be null");
    assertTrue(sha1.check(emptyPassword, hash), "Empty password should match its hash");
  }

  @Test
  void testGenerateHash_WithSpecialCharacters() {
    String specialPassword = "p@$$w0rd!@#$%^&*()";
    String hash = sha1.generateHash(specialPassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha1.check(specialPassword, hash), "Special characters password should match");
  }

  @Test
  void testGenerateHash_WithUnicode() {
    String unicodePassword = "密码пароль🔐";
    String hash = sha1.generateHash(unicodePassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha1.check(unicodePassword, hash), "Unicode password should match");
  }

  @Test
  void testCheck_ShouldBeCaseSensitive() {
    String password = "Password";
    String hash = sha1.generateHash(password);
    
    assertTrue(sha1.check("Password", hash), "Exact case should match");
    assertFalse(sha1.check("password", hash), "Different case should not match");
    assertFalse(sha1.check("PASSWORD", hash), "Different case should not match");
  }

  @Test
  void testGenerateHash_ShouldBeBase64Encoded() {
    String password = "test";
    String hash = sha1.generateHash(password);
    
    // Base64 characters are A-Z, a-z, 0-9, +, /, =
    assertTrue(hash.matches("^[A-Za-z0-9+/]+=*$"), "Hash should be valid base64");
  }

  @Test
  void testGenerateHash_WithLongPassword() {
    String longPassword = "a".repeat(1000);
    String hash = sha1.generateHash(longPassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha1.check(longPassword, hash), "Long password should match");
  }

  @Test
  void testGetHashingBaseAlgorithm_ShouldNotBeNull() {
    assertNotNull(sha1.getHashingBaseAlgorithm(), "Hashing algorithm should not be null");
    assertEquals("SHA-1", sha1.getHashingBaseAlgorithm().getAlgorithm(), 
        "Algorithm should be SHA-1");
  }

  @Test
  void testGenerateHash_KnownVector() {
    // Known SHA-1 base64 hash for "password"
    String password = "password";
    String expectedHash = "W6ph5Mm5Pz8GgiULbPgzG37mj9g=";
    String actualHash = sha1.generateHash(password);
    
    assertEquals(expectedHash, actualHash, "Hash should match known SHA-1 hash");
  }

  @Test
  void testCheck_WithDifferentPasswordsShouldReturnFalse() {
    String password1 = "password1";
    String password2 = "password2";
    String hash1 = sha1.generateHash(password1);
    
    assertFalse(sha1.check(password2, hash1), "Different passwords should not match");
  }
}
