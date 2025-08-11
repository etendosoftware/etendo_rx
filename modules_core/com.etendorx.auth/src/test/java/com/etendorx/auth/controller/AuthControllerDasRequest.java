package com.etendorx.auth.controller;

import com.etendorx.auth.auth.jwt.JwtRequest;
import com.jayway.jsonpath.JsonPath;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;
import com.etendorx.auth.test.utils.AuthTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerDasRequest {

  @BeforeAll
  static void startServices() throws IOException, InterruptedException, URISyntaxException {
    AuthTestUtils.startConfigServer();
    AuthTestUtils.startDASServer();
    AuthTestUtils.startAuthServer();
  }

  @Test
  void validCredentialsTokenCreation() {
    JwtRequest request = new JwtRequest();
    request.setUsername("admin");
    request.setPassword("admin");
    request.setSecret("1234");
    request.setService("auth");

    final ResponseEntity<String> response = getStringResponseEntity(request);

    assertEquals("", HttpStatus.OK, response.getStatusCode());
    String token = JsonPath.read(response.getBody(), "$.token");
    assertNotNull(token);
  }

  public static Stream<Arguments> params() {
    return Stream.of(
        Arguments.of("undefined", "pass123"), Arguments.of("admin", "und")
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  void invalidCredentials(String username, String password) {
    JwtRequest request = new JwtRequest();
    request.setUsername(username);
    request.setPassword(password);
    request.setSecret("1234");
    request.setService("auth");

    try {
      ResponseEntity<String> response = getStringResponseEntity(request);
      fail("It should be a 401 Unauthorized response, but it was: " + response.getStatusCode());
    } catch (HttpClientErrorException.Unauthorized ex) {
      assertEquals("", HttpStatus.UNAUTHORIZED, ex.getStatusCode());

      HttpStatusCode responseBody = ex.getStatusCode();
      assertEquals("", responseBody, HttpStatus.UNAUTHORIZED);
    }
  }


  private static @NotNull ResponseEntity<String> getStringResponseEntity(JwtRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    HttpEntity<String> entity = new HttpEntity<>(AuthControllerUtils.asJsonString(request), headers);
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.postForEntity("http://localhost:8094/api/authenticate", entity, String.class);
  }

  @AfterAll
  static void stopRunningServices() {
    AuthTestUtils.stopRunningServices();
  }
}
