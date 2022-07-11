package com.etendorx.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class JwtauthApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtauthApplication.class, args);
	}

}
