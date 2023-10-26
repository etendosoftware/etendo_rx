package com.etendorx.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan(basePackages = {
    "com.etendorx.clientrest.*",
    "com.etendorx.auth.*",
    "com.etendorx.utils.auth.key.context"
})
@EnableFeignClients(basePackages = {
    "com.etendorx.clientrest.*",
    "com.etendorx.auth.clientrest",
    "com.etendorx.auth.feign"
})
public class JwtauthApplication {

  public static void main(String[] args) {
    SpringApplication.run(JwtauthApplication.class, args);
  }

}
