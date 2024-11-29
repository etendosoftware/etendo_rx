package com.etendorx.das.configuration;

import com.etendorx.entities.jparepo.SMFSWS_ConfigRepository;
import com.etendorx.utils.auth.key.config.JwtClassicConfig;
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JwtClassicConfigurator {

  private final SMFSWS_ConfigRepository smfswsConfigRepository;

  public JwtClassicConfigurator(SMFSWS_ConfigRepository smfsws_configRepository) {
    smfswsConfigRepository = smfsws_configRepository;
  }

  @Bean
  public JwtClassicConfig jwtClassicConfig() {
    // Create a virtual user context for a system call in case there is no user in the context
    if(!AppContext.isUserInContext()) {
      UserContext userContext = new UserContext();
      AppContext.setCurrentUser(userContext);
    }
    JwtClassicConfig jwtClassicConfig = new JwtClassicConfig();
    var list = smfswsConfigRepository.findAll();
    if (list.iterator().hasNext()) {
      jwtClassicConfig.setPrivateKey(list.iterator().next().getPrivateKey());
      return jwtClassicConfig;
    }
    return null;
  }
}
