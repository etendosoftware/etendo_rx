package com.etendorx.utils.auth.token;

import com.etendorx.utils.auth.key.JwtKeyUtils;

import io.jsonwebtoken.lang.Assert;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.stream.Stream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenValidationTest {

  static PublicKey publicKeyStore;

  private String readFile(String locationFile) throws IOException {
    File file = new File(locationFile);
    if (!file.exists()) {
      throw new FileNotFoundException("The file location '" + locationFile + "' does not exists.");
    }
    return new String(Files.readAllBytes(file.toPath()));
  }

  @Test
  @Order(1)
  void readPublicKeyTest() throws IOException {
    File key = new File(getClass().getClassLoader().getResource("public_key.pem").getFile());
    PublicKey publicKey = JwtKeyUtils.readPublicKey(readFile(key.getAbsolutePath()));
    Assert.notNull(publicKey);
    publicKeyStore = publicKey;
  }

  public static Stream<Arguments> invalidTokens() {
    return Stream.of(
        Arguments.of(""),
        Arguments.of("invalidtoken"),
        Arguments.of(TokenUtils.INVALID_TOKEN_0),
        Arguments.of(TokenUtils.INVALID_TOKEN_1)
    );
  }

  @ParameterizedTest
  @MethodSource("invalidTokens")
  @Order(2)
  void invalidTokenTest(String token) {
    boolean isValidToken = JwtKeyUtils.isValidToken(publicKeyStore, token);
    Assert.isTrue(!isValidToken);
  }

  public static Stream<Arguments> validTokens() {
    return Stream.of(
        Arguments.of(TokenUtils.VALID_TOKEN_0),
        Arguments.of(TokenUtils.VALID_TOKEN_1)
    );
  }

  @ParameterizedTest
  @MethodSource("validTokens")
  @Order(3)
  void validTokenTest(String token) {
    boolean isValidToken = JwtKeyUtils.isValidToken(publicKeyStore, token);
    Assert.isTrue(isValidToken);
  }

}
