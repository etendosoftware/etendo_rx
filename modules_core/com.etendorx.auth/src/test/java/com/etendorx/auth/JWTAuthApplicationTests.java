package com.etendorx.auth;

import com.etendorx.auth.auth.AuthController;
import com.etendorx.auth.test.utils.AuthTestUtils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootTest
class JWTAuthApplicationTests {

  @Autowired
  private AuthController controller;

  @BeforeAll
  static void startConfig() throws IOException, InterruptedException, URISyntaxException {
    AuthTestUtils.startConfigServer();
  }

  @Test
  void contextLoads() {
    assertThat(controller).isNotNull();
  }

  @AfterAll
  static void stopConfig() {
    AuthTestUtils.stopRunningServices();
  }
}
