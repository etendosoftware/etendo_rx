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


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.hibernate.event.spi.PreUpdateEvent;
import org.junit.jupiter.api.Test;
import org.openbravo.model.ad.access.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.etendorx.das.EtendorxDasApplication;
import com.etendorx.entities.jparepo.ADUserRepository;
import com.etendorx.test.eventhandler.component.EventHandlerUser;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19091")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@Import(EtendorxDasApplication.class)
@AutoConfigureMockMvc
public class TestEventHandler {
  @SpyBean
  private EventHandlerUser component;

  @Autowired
  private ADUserRepository userRepository;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> {
      return "jdbc:postgresql://" + postgreSQLContainer.getCurrentContainerInfo().getNetworkSettings().getNetworks().entrySet().stream().findFirst().get().getValue().getGateway() + ":" + postgreSQLContainer.getMappedPort(
          5432) + "/etendo";
    });
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
  }

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
      DockerImageName.parse("etendo/etendodata:rx-1.2.1").asCompatibleSubstituteFor("postgres")
  )
      .withPassword("syspass")
      .withUsername("postgres")
      .withEnv("PGDATA", "/postgres")
      .withDatabaseName("etendo")
      .withExposedPorts(5432)
      .waitingFor(
          Wait.forLogMessage(".*database system is ready to accept connections*\\n", 1)
      );

  @Test
  void test() {
    User user = userRepository.findById("100").orElse(null);
    if (user != null) {
      user.setLastName("Test eventHandler");
      user = userRepository.save(user);
      verify(component).handlePreUpdate(eq(user), any(PreUpdateEvent.class));
    }
    assertNotNull(user);
  }
}
