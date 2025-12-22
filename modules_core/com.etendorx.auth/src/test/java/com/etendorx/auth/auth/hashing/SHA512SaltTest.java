package com.etendorx.auth.auth.hashing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SHA512SaltTest {

  private final SHA512Salt sha512Salt = new SHA512Salt();

  @Test
  void testGenerateHash_ShouldReturnProperFormat() {
    String password = "testPassword";
    String hash = sha512Salt.generateHash(password);
    
    assertNotNull(hash, "Hash should not be null");
    String[] parts = hash.split("\\$");
    assertEquals(3, parts.length, "Hash should have 3 parts: version$salt$hash");
    assertEquals("1", parts[0], "Version should be 1");
  }

  @Test
  void testGenerateHash_ShouldHaveDifferentSalts() {
    String password = "samePassword";
    String hash1 = sha512Salt.generateHash(password);
    String hash2 = sha512Salt.generateHash(password);
    
    String[] parts1 = hash1.split("\\$");
    String[] parts2 = hash2.split("\\$");
    
    assertNotEquals(parts1[1], parts2[1], "Salts should be different for each hash generation");
  }

  @Test
  void testGenerateHash_ShouldHaveDifferentHashes() {
    String password = "password123";
    String hash1 = sha512Salt.generateHash(password);
    String hash2 = sha512Salt.generateHash(password);
    
    assertNotEquals(hash1, hash2, "Complete hashes should be different due to different salts");
  }

  @Test
  void testCheck_ShouldReturnTrueForMatchingPassword() {
    String password = "correctPassword";
    String hash = sha512Salt.generateHash(password);
    
    assertTrue(sha512Salt.check(password, hash), "Password should match its own hash");
  }

  @Test
  void testCheck_ShouldReturnFalseForWrongPassword() {
    String password = "correctPassword";
    String wrongPassword = "wrongPassword";
    String hash = sha512Salt.generateHash(password);
    
    assertFalse(sha512Salt.check(wrongPassword, hash), "Wrong password should not match hash");
  }

  @Test
  void testGetAlgorithmVersion_ShouldReturnOne() {
    assertEquals(1, sha512Salt.getAlgorithmVersion(), "SHA512Salt algorithm version should be 1");
  }

  @Test
  void testGenerateHash_WithEmptyPassword() {
    String emptyPassword = "";
    String hash = sha512Salt.generateHash(emptyPassword);
    
    assertNotNull(hash, "Hash of empty password should not be null");
    assertTrue(sha512Salt.check(emptyPassword, hash), "Empty password should match its hash");
  }

  @Test
  void testGenerateHash_WithSpecialCharacters() {
    String specialPassword = "p@$$w0rd!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
    String hash = sha512Salt.generateHash(specialPassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha512Salt.check(specialPassword, hash), "Special characters password should match");
  }

  @Test
  void testGenerateHash_WithUnicode() {
    String unicodePassword = "密码пароль🔐";
    String hash = sha512Salt.generateHash(unicodePassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha512Salt.check(unicodePassword, hash), "Unicode password should match");
  }

  @Test
  void testGenerateHash_WithLongPassword() {
    String longPassword = "a".repeat(1000);
    String hash = sha512Salt.generateHash(longPassword);
    
    assertNotNull(hash, "Hash should not be null");
    assertTrue(sha512Salt.check(longPassword, hash), "Long password should match");
  }

  @Test
  void testCheck_ShouldBeCaseSensitive() {
    String password = "Password";
    String hash = sha512Salt.generateHash(password);
    
    assertTrue(sha512Salt.check("Password", hash), "Exact case should match");
    assertFalse(sha512Salt.check("password", hash), "Different case should not match");
    assertFalse(sha512Salt.check("PASSWORD", hash), "Different case should not match");
  }

  @Test
  void testGenerateHash_SaltShouldBeBase64() {
    String password = "test";
    String hash = sha512Salt.generateHash(password);
    String[] parts = hash.split("\\$");
    String salt = parts[1];
    
    // Base64 characters are A-Z, a-z, 0-9, +, / (without padding in this case)
    assertTrue(salt.matches("^[A-Za-z0-9+/]+$"), "Salt should be valid base64 without padding");
  }

  @Test
  void testCheck_WithTamperedHash() {
    String password = "password";
    String hash = sha512Salt.generateHash(password);
    String tamperedHash = hash.substring(0, hash.length() - 1) + "X";
    
    assertFalse(sha512Salt.check(password, tamperedHash), "Tampered hash should not match");
  }

  @Test
  void testCheck_WithTamperedSalt() {
    String password = "password";
    String hash = sha512Salt.generateHash(password);
    String[] parts = hash.split("\\$");
    String tamperedHash = parts[0] + "$differentSalt$" + parts[2];
    
    assertFalse(sha512Salt.check(password, tamperedHash), "Hash with tampered salt should not match");
  }

  @Test
  void testGetHashingBaseAlgorithm_ShouldNotBeNull() {
    assertNotNull(sha512Salt.getHashingBaseAlgorithm(), "Hashing algorithm should not be null");
    assertEquals("SHA-512", sha512Salt.getHashingBaseAlgorithm().getAlgorithm(), 
        "Algorithm should be SHA-512");
  }
}
