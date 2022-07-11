package com.etendorx.auth;

import com.etendorx.auth.auth.AuthController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtauthApplicationTests {

	@Autowired
	private AuthController controller;

	@Test
	void contextLoads() {
		assertThat(controller).isNotNull();
	}

}
