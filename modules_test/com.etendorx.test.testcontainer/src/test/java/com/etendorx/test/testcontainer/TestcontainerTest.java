package com.etendorx.test.testcontainer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest
@Testcontainers
public class TestcontainerTest {

  private static final PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>(
      "postgres:14").withExposedPorts(5432)
      .withUsername("postgres").withPassword("postgres").withDatabaseName("postgres")
      .waitingFor(
          Wait.forLogMessage(".*database system is ready to accept connections*\\n", 1)
      );

  @BeforeAll
  static void beforeAll() {
    postgreSqlContainer.start();
  }

  @Test
  void test() {
    Assertions.assertEquals(1, 1);
  }

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSqlContainer::getJdbcUrl);
    registry.add("spring.datasource.password", postgreSqlContainer::getPassword);
    registry.add("spring.datasource.username", postgreSqlContainer::getUsername);
  }
}
