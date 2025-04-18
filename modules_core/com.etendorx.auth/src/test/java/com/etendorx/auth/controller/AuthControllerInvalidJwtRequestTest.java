package com.etendorx.auth.controller;

import com.etendorx.auth.auth.AuthService;
import com.etendorx.auth.auth.jwt.JwtRequest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.etendorx.auth.test.utils.AuthTestUtils.getRootProjectPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerInvalidJwtRequestTest {

  @Autowired
  private MockMvc mockMvc;

  static Process configProcess;

  @BeforeAll
  static void startConfig() throws IOException, InterruptedException, URISyntaxException {
    final String rootProjectPath = getRootProjectPath();
    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "/tmp/com.etendorx.configserver-2.3.0.jar");
    Map<String, String> env = pb.environment();
    env.put("SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCHLOCATIONS", "file://" + rootProjectPath + "/rxconfig");
    env.put("SPRING_PROFILES_ACTIVE", "native");
    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    configProcess = pb.start();

    Thread.sleep(25000);
  }

  private static Stream<Arguments> invalidJwtRequestParams() {
    return Stream.of(
        // Undefined username
        Arguments.of(null, null, AuthService.UNDEFINED_USERNAME_MESSAGE),
        Arguments.of("", "", AuthService.UNDEFINED_USERNAME_MESSAGE),
        Arguments.of("", null, AuthService.UNDEFINED_USERNAME_MESSAGE),
        Arguments.of("", "pass123", AuthService.UNDEFINED_USERNAME_MESSAGE),
        // Undefined password
        Arguments.of("user123", null, AuthService.UNDEFINED_PASSWORD_MESSAGE),
        Arguments.of("user123", "", AuthService.UNDEFINED_PASSWORD_MESSAGE));
  }

  @ParameterizedTest
  @MethodSource("invalidJwtRequestParams")
  void invalidJwtRequest(String username, String password, String errorMessage)
      throws Exception {
    JwtRequest request = new JwtRequest();
    request.setUsername(username);
    request.setPassword(password);
    request.setSecret("1234");
    request.setService("auth");

    ResultActions resultActions = this.mockMvc.perform(
        MockMvcRequestBuilders.post("/api/authenticate")
            .content(AuthControllerUtils.asJsonString(request))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON));

    resultActions.andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(
            result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
        .andExpect(result -> {
          assertEquals(
              Objects.requireNonNull((ResponseStatusException) result.getResolvedException())
                  .getReason(), errorMessage);
        });
  }

  @AfterAll
  static void stopConfig() {
    if (configProcess != null) {
      configProcess.destroy();
    }
  }
}
