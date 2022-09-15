/*
 * Copyright 2022  Futit Services SL
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

package com.etendorx.das.test;

import com.etendorx.entities.jparepo.ADUserRepository;
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


  public static Stream<Arguments> validBusinessPartnerCategoryName() {
    return Stream.of(
      Arguments.of("100", null, null),
      Arguments.of("167450A5BB284AB29C4FEF039E98C963", null, null),
      Arguments.of("26EF171A1D75485083D276D49AAACD45", null, null),
      Arguments.of("5A79667096964E83A6985D549C988275", null, null),
      Arguments.of("D249DE7A14FB4F77BC056A3738A63477", null, null),
      Arguments.of("E12DC7B3FF8C4F64924A98195223B1F8", null, null),
      Arguments.of("20C5D31133D949F0BD25412DD1069612", "Fruit & Bio is Life, Inc.", "Supplier"),
      Arguments.of("2748452130E84FF0B1A8292D88570F8F", "Moon Light Restaurants, Co.", "Customer - Tier 2"),
      Arguments.of("33FE57CFE5BE4774B9B9EDFD8E27BCAE", "Bebidas Alegres, S.L.", "Supplier"),
      Arguments.of("50A34002FDA34FC58F1319E25EDA4E3A", "La Fruta es la Vida, S.L.", "Supplier"),
      Arguments.of("545A04EE5EF94E9B967536140226793F", "Healthy Food Supermarkets, Co.", "Customer - Tier 1"),
      Arguments.of("6628F632D484407CBCBD8E71C123A263", "Be Soft Drinker, Inc.", "Supplier"),
      Arguments.of("6822CC074A064323B639A7087ED14859", "Refrescos Naturales, S.A.", "Supplier"),
      Arguments.of("6A3D3D6A808C455EAF1DAB48058FDBF4", "Restaurantes Luna Llena, S.A.", "Customer - Tier 2"),
      Arguments.of("6B1E8F0CA1524850AB3A7F9AE475A16F", "Alimentos y Supermercados, S.A", "Customer - Tier 1"),
      Arguments.of("81FE8AF4E1EF4488B0D92143942C79C3", "Sleep Well Hotels, Co.", "Customer - Tier 3"),
      Arguments.of("8537B1F5669E423ABA79F1F57B1E4222", "John Smith", "Employee"),
      Arguments.of("A6EA3469E33544D184F836D97F274E0A", "Luca Simone", "Employee"),
      Arguments.of("BFEF159DC6BF4178913C9E38FB706155", "Happy Drinks, Inc.", "Supplier"),
      Arguments.of("C3503BEFB3CB4848A674284A656163B9", "Javier Martín", "Employee"),
      Arguments.of("CA5D537DFD014F15BFFBA0DB81E1A379", "Hoteles Buenas Noches, S.A.", "Customer - Tier 3"),
      Arguments.of("CADCDC3549FB4201B5F24E4C03AD2349", "Juan López", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C1D702E6", "Vendor A", "Vendor"),
      Arguments.of("4028E6C72959682B01295F40C30F02EA", "Vendor B", "Vendor"),
      Arguments.of("4028E6C72959682B01295F40C43802EF", "Customer A", "Customer"),
      Arguments.of("4028E6C72959682B01295F40C52302F3", "Customer B", "Customer"),
      Arguments.of("4028E6C72959682B01295F40C63C02F8", "Employee A", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C72602FC", "Employee B", "Employee"),
      Arguments.of("4028E6C72959682B01295F40C84F0301", "Salesman A", "Salesman"),
      Arguments.of("4028E6C72959682B01295F40C93A0305", "Salesman B", "Salesman"),
      Arguments.of("4028E6C72959682B01295F40CA62030A", "Creditor", "Financial Accounts")
    );
  }

  @ParameterizedTest
  @MethodSource("validBusinessPartnerCategoryName")
  public void whenRestReadBusinessPartnerCategoryName(String id, String businessPartnerName, String businessPartnerCategoryName) throws Exception {
    var result = mockMvc.perform(
      get("/ADUser/" + id + "?projection=test")
    );
    result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    result.andExpect(MockMvcResultMatchers.jsonPath("$.businessPartnerName").value(businessPartnerName));
    result.andExpect(MockMvcResultMatchers.jsonPath("$.businessPartnerCategoryName").value(businessPartnerCategoryName));
  }


  public static Stream<Arguments> validInvoiceLineProjection() {
    return Stream.of(
            Arguments.of("0002165C32244FF382FE46E57C755421", "F8383209982D467EBAA87460953694D1", "Zumo de Pera 0,5L"),
            Arguments.of("0004ABE4F2B84554868D0F410F0E92E5", "C970393BDF6C43E2B030D23482D88EED", "Zumo de Piña 0,5L"),
            Arguments.of("000A407AC9BE4FB08AEC78577D1E4FA2", "74D47F21C81746239B1A08ECADC98BF1", "Rose wine"),
            Arguments.of("0013F39CF44441DD8C62C4810F914323", "BDE2F1CF46B54EF58D33E20A230DA8D2", "Agua sin Gas 1L")
    );
  }

  @ParameterizedTest
  @MethodSource("validInvoiceLineProjection")
  public void whenRestReadInvoiceLineCustomProjection(String id, String invoiceLineProductId, String invoiceLineProductName) throws Exception {
    var result = mockMvc.perform(
            get("/InvoiceLine/" + id + "?projection=test")
    );
    result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    result.andExpect(MockMvcResultMatchers.jsonPath("$.productId").value(invoiceLineProductId));
    result.andExpect(MockMvcResultMatchers.jsonPath("$.product").value(invoiceLineProductName));
  }

}
