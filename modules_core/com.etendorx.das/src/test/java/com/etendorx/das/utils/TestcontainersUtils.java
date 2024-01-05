package com.etendorx.das.utils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.util.ObjectUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestcontainersUtils {
  private TestcontainersUtils() {
  }

  public static void setProperties(DynamicPropertyRegistry registry,
      PostgreSQLContainer<?> postgreSQLContainer) {
    registry.add("spring.datasource.url", () -> {
      String envTestcontainerIp = System.getenv("TESTCONTAINERS_IP");
      if (ObjectUtils.isEmpty(envTestcontainerIp)) {
        return postgreSQLContainer.getJdbcUrl();
      } else {
        if (envTestcontainerIp.compareTo("GATEWAY") == 0) {
          return "jdbc:postgresql://" + postgreSQLContainer.getCurrentContainerInfo()
              .getNetworkSettings()
              .getNetworks()
              .entrySet()
              .stream()
              .findFirst()
              .get()
              .getValue()
              .getGateway() + ":" + postgreSQLContainer.getMappedPort(5432) + "/etendo";
        }
      }
      throw new RuntimeException("TESTCONTAINERS_IP method has and invalid value");
    });
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
  }

  public static PostgreSQLContainer<?> createDBContainer() {
    String envEtendodataTag = System.getenv("ETENDODATA_TAG");
    if (envEtendodataTag == null) {
      try {
        Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD"); //NOSONAR
        process.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        envEtendodataTag = "rx-" + reader.readLine().replace("/", "-");
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Error getting git branch name"); //NOSONAR
      } catch (Exception e) {
        throw new RuntimeException("Error getting git branch name"); //NOSONAR
      }
    }
    return new PostgreSQLContainer<>(DockerImageName.parse("etendo/etendodata:" + envEtendodataTag)
        .asCompatibleSubstituteFor("postgres")).withPassword("syspass")
        .withUsername("postgres")
        .withEnv("PGDATA", "/postgres")
        .withDatabaseName("etendo")
        .withExposedPorts(5432)
        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections*\\n", 1));
  }

  public static String getJsonFromFile(String jsonFile) {
    try {
      var f = new File(TestcontainersUtils.class.getResource(jsonFile).getFile());
      return Files.readString(f.toPath(), UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Error reading json file: " + jsonFile);
    }
  }
}
