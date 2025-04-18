package com.etendorx.auth.auth;

import com.etendorx.auth.auth.jwt.JwtRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static com.etendorx.auth.auth.AuthService.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class JwtRequestValidationTest {

  private static final String PASSWORD = "password";
  private static final String SERVICE = "service";
  private static final String SECRET = "secret";
  private static final String USERNAME = "username";
  @InjectMocks
  private AuthService jwtRequestValidation;

  @Test
  void shouldThrowExceptionWhenUsernameIsEmpty() {
    JwtRequest jwtRequest = new JwtRequest("", PASSWORD, SERVICE, SECRET);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> jwtRequestValidation.validateJwtRequest(jwtRequest));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo(UNDEFINED_USERNAME_MESSAGE);
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsEmpty() {
    JwtRequest jwtRequest = new JwtRequest(USERNAME, "", SERVICE, SECRET);
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> jwtRequestValidation.validateJwtRequest(jwtRequest));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo(UNDEFINED_PASSWORD_MESSAGE);
  }

  @Test
  void shouldThrowExceptionWhenServiceIsEmpty() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new JwtRequest(USERNAME, PASSWORD, "", SECRET));
    assertThat(exception.getMessage()).isEqualTo("Service cannot be null or empty");
  }

  @Test
  void shouldThrowExceptionWhenSecretIsEmpty() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new JwtRequest(USERNAME, PASSWORD, SERVICE, ""));
    assertThat(exception.getMessage()).isEqualTo("Secret cannot be null or empty");
  }
}
