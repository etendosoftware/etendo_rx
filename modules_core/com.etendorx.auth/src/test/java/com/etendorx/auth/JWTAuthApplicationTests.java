package com.etendorx.auth;

import com.etendorx.auth.auth.AuthController;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.etendorx.auth.test.utils.AuthTestUtils.getRootProjectPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@SpringBootTest
class JWTAuthApplicationTests {


  @Autowired
  private AuthController controller;

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

  @Test
  void contextLoads() {
    AuthController controller = new AuthController();
    assertThat(controller).isNotNull();
  }

  @AfterAll
  static void stopConfig() {
    if (configProcess != null) {
      configProcess.destroy();
    }
  }
}
