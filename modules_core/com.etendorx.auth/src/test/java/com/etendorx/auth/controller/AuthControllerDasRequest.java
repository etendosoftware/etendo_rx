package com.etendorx.auth.controller;

import com.etendorx.auth.auth.AuthService;
import com.etendorx.auth.auth.jwt.JwtRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerDasRequest {

  @Autowired
  private MockMvc mockMvc;

  @Value("${das.endpoint}")
  private String dasEndpoint;

  private WireMockServer wireMockServer;

  void configureMockServer() {
    Map<String, Object> url = AuthControllerUtils.parseUrl(dasEndpoint);
    String host = (String) url.get("host");
    int port = (Integer) url.get("port");

    wireMockServer = new WireMockServer(port);
    wireMockServer.start();
    configureFor(host, port);

    addMockUrl(String.valueOf(port));
  }

  void addMockUrl(String port) {
    UrlPattern urlSearchUser = WireMock.urlEqualTo(
        "/ADUser/search/searchByUsername?username=admin&active=true&projection=auth");
    // Mock User
    stubFor(WireMock.get(urlSearchUser)
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaTypes.HAL_JSON_VALUE)
            .withBody(AuthControllerUtils.getSearchUserResponseBody(port))));

    UrlPattern urlUndefinedSearchUser = WireMock.urlEqualTo(
        "/ADUser/search/searchByUsername?username=undefined&active=true&projection=auth");
    // Mock User
    stubFor(WireMock.get(urlUndefinedSearchUser)
        .willReturn(WireMock.aResponse()
            .withStatus(200)
            .withHeader("Content-Type", MediaTypes.HAL_JSON_VALUE)
            .withBody(AuthControllerUtils.getUsernameNotFoundResponseBody(port))));
  }

  @Test
  void validCredentialsTokenCreation() throws Exception {
    configureMockServer();

    JwtRequest request = new JwtRequest();
    request.setUsername("admin");
    request.setPassword("admin");

    ResultActions resultActions = this.mockMvc.perform(MockMvcRequestBuilders
        .post("/api/authenticate")
        .content(AuthControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));

    resultActions.andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").exists());

    wireMockServer.stop();
  }

  public static Stream<Arguments> params() {
    return Stream.of(
        // Undefined username
        Arguments.of("undefined", "pass123"),
        Arguments.of("admin", "und")

    );
  }

  @ParameterizedTest
  @MethodSource("params")
  void invalidCredentials(String username, String password) throws Exception {
    configureMockServer();

    JwtRequest request = new JwtRequest();
    request.setUsername(username);
    request.setPassword(password);

    ResultActions resultActions = this.mockMvc.perform(MockMvcRequestBuilders
        .post("/api/authenticate")
        .content(AuthControllerUtils.asJsonString(request))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));

    resultActions.andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
        .andExpect(result -> {
          Assertions.assertEquals(
              Objects.requireNonNull((ResponseStatusException) result.getResolvedException()).getReason(),
              AuthService.UNAUTHORIZED_MESSAGE);
        });

    wireMockServer.stop();
  }

}
