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

  @InjectMocks
  private AuthService jwtRequestValidation;

  @Test
  void shouldThrowExceptionWhenUsernameIsEmpty() {
    JwtRequest jwtRequest = new JwtRequest("", "password", "service", "secret");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> jwtRequestValidation.validateJwtRequest(jwtRequest));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo(UNDEFINED_USERNAME_MESSAGE);
  }

  @Test
  void shouldThrowExceptionWhenPasswordIsEmpty() {
    JwtRequest jwtRequest = new JwtRequest("username", "", "service", "secret");
    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> jwtRequestValidation.validateJwtRequest(jwtRequest));
    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(exception.getReason()).isEqualTo(UNDEFINED_PASSWORD_MESSAGE);
  }

  @Test
  void shouldThrowExceptionWhenServiceIsEmpty() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new JwtRequest("username", "password", "", "secret"));
    assertThat(exception.getMessage()).isEqualTo("Service cannot be null or empty");
  }

  @Test
  void shouldThrowExceptionWhenSecretIsEmpty() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new JwtRequest("username", "password", "service", ""));
    assertThat(exception.getMessage()).isEqualTo("Secret cannot be null or empty");
  }
}
