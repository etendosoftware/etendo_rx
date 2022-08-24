package com.etendorx.das.test;

import com.etendorx.entities.jparepo.ADUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class RestCallTest {

  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
  }

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
    DockerImageName.parse("futit/etendodata:latest").asCompatibleSubstituteFor("postgres")
  )
    .withPassword("syspass")
    .withUsername("postgres")
    .withEnv("PGDATA", "/postgres")
    .withDatabaseName("etendo")
    .waitingFor(new HostPortWaitStrategy());

  public static Stream<Arguments> validRequestParams() {
    return Stream.of(
      // Undefined username
      Arguments.of("ADUser", "0", "System"),
      Arguments.of("ADUser", "100", "Admin"),
      Arguments.of("ADUser", "167450A5BB284AB29C4FEF039E98C963", "F&B ES User"),
      Arguments.of("ADUser", "20C5D31133D949F0BD25412DD1069612", " Rome"),
      Arguments.of("ADUser", "26EF171A1D75485083D276D49AAACD45", "F&BESRSUser"),
      Arguments.of("ADUser", "2748452130E84FF0B1A8292D88570F8F", "Joe Matt"),
      Arguments.of("ADUser", "5A79667096964E83A6985D549C988275", "F&B US User"),
      Arguments.of("ADUser", "6628F632D484407CBCBD8E71C123A263", "Tom"),
      Arguments.of("ADUser", "D249DE7A14FB4F77BC056A3738A63477", "F&BUSECUser"),
      Arguments.of("ADUser", "E12DC7B3FF8C4F64924A98195223B1F8", "F&BUser"),
      Arguments.of("ADUser", "4028E6C72959682B01295F40C1D702E6", "John"),
      Arguments.of("ADUser", "4028E6C72959682B01295F40C30F02EA", "Albert"),
      Arguments.of("Product", "0DC5C5281B3643DEAB978EB04139516B", "Orange Juice bio"),
      Arguments.of("Product", "19857ACFC55D45E2AECAF85B2506C3DB", "Alquiler de oficina"),
      Arguments.of("Product", "20EA4222B741434A9203471A3B29C343", "Vino Blanco 0,75L"),
      Arguments.of("Product", "20FBF069AC804DE9BF16670000B9562E", "Cherry Cola"),
      Arguments.of("Product", "2A81EB0F5C4F40F6A6E04A938F8DCFF9", "Office Rental"),
      Arguments.of("Product", "34560A057833457D962F7A573F76F5BB", "Ale Beer"),
      Arguments.of("Product", "3BD16BA206D24E41B56E0964854220EA", "Limonada 0,5L"),
      Arguments.of("Product", "3DBB480253094C99A4408923F69806D7", "Electricity"),
      Arguments.of("Product", "407E8BA62FAE4905B0D9F66502B83746", "Sistemas de información"),
      Arguments.of("Product", "4208D40D884B40A7AA6495031D3D4B55", "Red Wine")
    );
  }

  @ParameterizedTest
  @MethodSource("validRequestParams")
  public void whenRestRead(String model, String id, String name) throws Exception {
    var result = mockMvc.perform(
      get("/" + model + "/" + id + "?projection=default")
    );
    result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    result.andExpect(MockMvcResultMatchers.jsonPath("$.name").value(name));
  }

  public static Stream<Arguments> invalidRequestParams() {
    return Stream.of(
      // Undefined username
      Arguments.of("123"),
      Arguments.of("456"),
      Arguments.of("789"),
      Arguments.of("0AB"),
      Arguments.of("CDE"),
      Arguments.of("FGH"),
      Arguments.of("IJK"),
      Arguments.of("LMN"),
      Arguments.of("OPQ"),
      Arguments.of("RST"),
      Arguments.of("UVW")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidRequestParams")
  public void whenRestReadFails(String id) throws Exception {
    var result = mockMvc.perform(
      get("/ADUser/" + id + "?projection=default")
    );
    result.andExpect(MockMvcResultMatchers.status().is4xxClientError());
    result.andExpect(response -> {
      assert response.getResponse().getContentAsString().length() == 0;
    });
  }

}
