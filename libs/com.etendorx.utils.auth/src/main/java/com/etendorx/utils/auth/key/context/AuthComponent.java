package com.etendorx.utils.auth.key.context;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
public class AuthComponent {
  @Bean
  @RequestScope
  public UserContext userContextBean() {
    return new UserContext();
  }
}
