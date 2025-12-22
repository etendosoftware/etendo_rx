package com.etendorx.auth.auth.hashing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {

  @Test
  void testGenerateHash_ShouldReturnNonNullHash() {
    String plainText = "testPassword123";
    String hash = PasswordHash.generateHash(plainText);
    
    assertNotNull(hash, "Generated hash should not be null");
    assertFalse(hash.isEmpty(), "Generated hash should not be empty");
  }

  @Test
  void testGenerateHash_ShouldContainVersionPrefix() {
    String plainText = "myPassword";
    String hash = PasswordHash.generateHash(plainText);
    
    assertTrue(hash.startsWith("1$"), "Hash should start with version prefix '1$'");
  }

  @Test
  void testGenerateHash_ShouldBeDifferentForSamePassword() {
    String plainText = "samePassword";
    String hash1 = PasswordHash.generateHash(plainText);
    String hash2 = PasswordHash.generateHash(plainText);
    
    assertNotEquals(hash1, hash2, "Two hashes of same password should be different due to random salt");
  }

  @Test
  void testMatches_ShouldReturnTrueForCorrectPassword() {
    String plainText = "correctPassword";
    String hash = PasswordHash.generateHash(plainText);
    
    assertTrue(PasswordHash.matches(plainText, hash), "Password should match its hash");
  }

  @Test
  void testMatches_ShouldReturnFalseForIncorrectPassword() {
    String plainText = "correctPassword";
    String wrongPassword = "wrongPassword";
    String hash = PasswordHash.generateHash(plainText);
    
    assertFalse(PasswordHash.matches(wrongPassword, hash), "Wrong password should not match hash");
  }

  @Test
  void testMatches_ShouldWorkWithSHA1Hash() {
    // SHA-1 hash format (version 0, no salt prefix)
    String plainText = "testValue123";
    // Pre-computed SHA-1 hash for testing legacy support
    String sha1Hash = "zBhFDvNOJc0fXyTaL+IkKMEz+zQ=";
    
    assertTrue(PasswordHash.matches(plainText, sha1Hash), "Should match SHA-1 hash format");
  }

  @Test
  void testMatches_ShouldWorkWithSHA512Hash() {
    String plainText = "testPassword";
    String hash = PasswordHash.generateHash(plainText);
    
    assertTrue(hash.contains("$"), "SHA-512 hash should contain salt separator");
    assertTrue(PasswordHash.matches(plainText, hash), "Should match SHA-512 hash format");
  }

  @Test
  void testGetAlgorithm_ShouldReturnSHA1ForVersionZero() {
    String sha1Hash = "someHashWithoutPrefix";
    HashingAlgorithm algorithm = PasswordHash.getAlgorithm(sha1Hash);
    
    assertNotNull(algorithm, "Algorithm should not be null");
    assertEquals(0, algorithm.getAlgorithmVersion(), "Should return SHA-1 algorithm (version 0)");
  }

  @Test
  void testGetAlgorithm_ShouldReturnSHA512ForVersionOne() {
    String sha512Hash = "1$someSalt$someHash";
    HashingAlgorithm algorithm = PasswordHash.getAlgorithm(sha512Hash);
    
    assertNotNull(algorithm, "Algorithm should not be null");
    assertEquals(1, algorithm.getAlgorithmVersion(), "Should return SHA-512 algorithm (version 1)");
  }

  @Test
  void testGetAlgorithm_ShouldThrowExceptionForUnsupportedVersion() {
    String invalidHash = "99$someSalt$someHash";
    
    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> PasswordHash.getAlgorithm(invalidHash),
        "Should throw exception for unsupported algorithm version"
    );
    
    assertTrue(exception.getMessage().contains("99"), "Exception message should mention the invalid version");
  }

  @Test
  void testGenerateHash_WithEmptyString() {
    String emptyString = "";
    String hash = PasswordHash.generateHash(emptyString);
    
    assertNotNull(hash, "Hash of empty string should not be null");
    assertTrue(PasswordHash.matches(emptyString, hash), "Empty string should match its hash");
  }

  @Test
  void testGenerateHash_WithSpecialCharacters() {
    String specialChars = "p@ssw0rd!#$%^&*()";
    String hash = PasswordHash.generateHash(specialChars);
    
    assertNotNull(hash, "Hash of special characters should not be null");
    assertTrue(PasswordHash.matches(specialChars, hash), "Special characters should match hash");
  }

  @Test
  void testGenerateHash_WithUnicodeCharacters() {
    String unicode = "пароль密码🔒";
    String hash = PasswordHash.generateHash(unicode);
    
    assertNotNull(hash, "Hash of unicode should not be null");
    assertTrue(PasswordHash.matches(unicode, hash), "Unicode should match hash");
  }

  @Test
  void testMatches_ShouldBeCaseSensitive() {
    String lowerCase = "password";
    String upperCase = "PASSWORD";
    String hash = PasswordHash.generateHash(lowerCase);
    
    assertTrue(PasswordHash.matches(lowerCase, hash), "Lowercase should match");
    assertFalse(PasswordHash.matches(upperCase, hash), "Uppercase should not match");
  }
}
