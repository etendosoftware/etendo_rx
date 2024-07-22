/*
 * Copyright 2023  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.etendorx.test.eventhandler;

import com.etendorx.das.EtendorxDasApplication;
import com.etendorx.entities.jparepo.ADUserRepository;
import com.etendorx.test.eventhandler.component.EventHandlerUser;
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;
import org.hibernate.event.spi.PreUpdateEvent;
import org.junit.jupiter.api.Test;
import org.openbravo.model.ad.access.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19091")
@ContextConfiguration
@Import(EtendorxDasApplication.class)
@SpringBootConfiguration
public class TestEventHandler {
  @SpyBean
  private EventHandlerUser component;

  @Autowired
  private ADUserRepository userRepository;

  @org.springframework.boot.test.context.TestConfiguration
  static class RepositoryTestConfiguration {
    @Bean
    public UserContext userContext() {
      return new UserContext();
    }
  }

  @Autowired
  private UserContext userContext;

  @Test
  void testUpdateUserAndExecuteEventHandler() {
    AppContext.setCurrentUser(userContext);
    User user = userRepository.findById("100").orElse(null);
    if (user != null) {
      user.setLastName("Test eventHandler");
      user = userRepository.save(user);
      verify(component).handlePreUpdate(eq(user), any(PreUpdateEvent.class));
    }
    assertNotNull(user);
  }
}
