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

import com.etendorx.das.utils.TestcontainersUtils;
import com.etendorx.entities.jparepo.ADUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import org.skyscreamer.jsonassert.JSONCompareMode;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "grpc.server.port=19090", "scan.basePackage=com.etendorx.integration.to_openbravo",
    "public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAocs6752BX8E9sUSkP0nnQlp9QNtTsBHB/jFZOro2ayCf203u3DHCPrLpLDZyrqAasIRRxKAAMNfmhl7/Hgg5FKeLp8rKEavlDTblVfVLvBmYpoJMxE2RumW4SdyP56LNnSlY49srflyiJyd9w+m0vVxMpXPT1RWTv+FJibVB8asqyUWW5sJgQ8Cr3PLI8KDCcwSpjlkkack3vB2ZiFtZVPntj4C6+/o5hcPgUeLVOFjH1H9zJP/ELLcueZtSbRo4J1CJsLUyY3ZCIk84wZwfielygT6Yl3tNqGGnxm7moXO8+y5uJZymoMqEhV5OnlolpAb/VuGZviv932fWzEMRjwIDAQAB" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class MappingsRestTest {

  public static final String X_TOKEN = "X-TOKEN";
  private static final String INVOICE_MODEL = "Invoice";
  private static final String ORDER_MODEL = "Order";
  private static final String PRODUCT_MODEL = "Product";
  private static final String PRICINGPRICELIST_MODEL = "PricingPriceList";
  private static final String PRICINGPRICELISTVERSION_MODEL = "PricingPriceListVersion";
  private static final String PRODUCTPRICE_MODEL = "ProductPrice";
  private static final String BASE_URL = "/obmap/";
  private static final String DATE_FORMAT = "?_dateFormat=yyyy-MM-dd HH:mm:ss";
  private static final String TIME_ZONE = "&_timeZone=GMT-3";
  private static final String ACCEPT_HEADER = "application/json; charset=UTF-8";
  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjk5NDcyNDc3LCJhZF91c2VyX2lkIjoiMTY3NDUwQTVCQjI4NEFCMjlDNEZFRjAzOUU5OEM5NjMiLCJhZF9jbGllbnRfaWQiOiIyM0M1OTU3NUI5Q0Y0NjdDOTYyMDc2MEVCMjU1QjM4OSIsImFkX29yZ19pZCI6IkU0NDNBMzE5OTJDQjQ2MzVBRkNBRUFCRTcxODNDRTg1IiwiYWRfcm9sZV9pZCI6IkYzMTk2QTMwQjUzQTQyNzc4NzI3QjI4NTJGRjkwQzI0Iiwic2VhcmNoX2tleSI6Im9iY29ubiIsInNlcnZpY2VfaWQiOiI1OTJBRDg3OTBFQUQ0M0FCOEY0RUI0NEFFODNDNzkyNyJ9.Ghjdvd6YDHDgOfY64zC5OtrbdQvf8ZASO2B33jknSqFew7uVnz9XDWv-mZqAiUfBcu2aSGERNU7acEMy0XsQHctGdU9_B5430hO_9kpumLeqbKHrxv4nFX2JvjaVSbu4ZFsZh-lfuPyTT6SoOmerITHC-0sMKCPHoKoJ6Z35xb1hYk1OWilIGPpKFVYbBm4lppNgAMYjywhWkzVeAczkji-g4U7KilhhPJPOnx75b-6EgDe4jxcxF3z_QtXS5sS9IQFrTOYi1VpiDvkVw3ZPOPHLh6ntXELLVpYhWHsJwXtYxfdaq4kTkIHpKHW4J41tVDC-7EFaIXohGAB02wQG0A";

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    TestcontainersUtils.setProperties(registry, postgreSQLContainer);
  }

  @Container
  public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
      DockerImageName.parse("etendo/etendodata:rx-1.2.1")
          .asCompatibleSubstituteFor("postgres")).withPassword("syspass")
      .withUsername("postgres")
      .withEnv("PGDATA", "/postgres")
      .withDatabaseName("etendo")
      .withExposedPorts(5432)
      .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections*\\n", 1));


  private void performGetRequestAndAssertResponse(String model, String id, String jsonFile) throws Exception {
    String expectedJsonData = TestcontainersUtils.getJsonFromFile(jsonFile);
    var result = mockMvc.perform(get(BASE_URL + model + "/" + id + DATE_FORMAT + TIME_ZONE)
            .header(X_TOKEN, TOKEN)
            .header("Accept", ACCEPT_HEADER)
            .characterEncoding(UTF_8.toString()))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful()).andReturn();
    assertResponse(result, expectedJsonData);
  }

  private void performPostRequestAndAssertResponse(String model, String jsonFile, String responseJsonFile) throws Exception {
    String postedJsonData = TestcontainersUtils.getJsonFromFile(jsonFile);
    String expectedJsonData = TestcontainersUtils.getJsonFromFile(responseJsonFile);
    var result = mockMvc.perform(post(BASE_URL + model + DATE_FORMAT + TIME_ZONE + "&_triggerEnabled=false")
            .header(X_TOKEN, TOKEN)
            .header("Accept", ACCEPT_HEADER)
            .content(postedJsonData))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful()).andReturn();
    assertResponse(result, expectedJsonData);
  }

  private void assertResponse(MvcResult result, String expectedJsonData) throws Exception {
    String actualJsonData = result.getResponse().getContentAsString();
    assertEquals(expectedJsonData, actualJsonData, JSONCompareMode.LENIENT);
  }

  @Test
  void shouldReturnInvoiceWhenRequested() throws Exception {
    String id = "5057F53393774AC3A952F9143426A922";
    String jsonFile = "/jsons/invoice.read.json";
    performGetRequestAndAssertResponse(INVOICE_MODEL, id, jsonFile);
  }

  @Test
  void shouldCreateInvoiceWhenPosted() throws Exception {
    String jsonFile = "/jsons/invoice.write.json";
    String responseJsonFile = "/jsons/invoice.write.response.json";
    performPostRequestAndAssertResponse(INVOICE_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnOrderWhenRequested() throws Exception {
    String id = "26593FEBBE40426991B6C4475DCE5BEE";
    String jsonFile = "/jsons/order.read.json";
    performGetRequestAndAssertResponse(ORDER_MODEL, id, jsonFile);
  }

  @Test
  void shouldCreateOrderWhenPosted() throws Exception {
    String jsonFile = "/jsons/order.write.json";
    String responseJsonFile = "/jsons/order.write.response.json";
    performPostRequestAndAssertResponse(ORDER_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnProductWhenRequested() throws Exception {
    String id = "0DC5C5281B3643DEAB978EB04139516B";
    String jsonFile = "/jsons/product.read.json";
    performGetRequestAndAssertResponse(PRODUCT_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnProductWhenPosted() throws Exception {
    String jsonFile = "/jsons/product.write.json";
    String responseJsonFile = "/jsons/product.write.response.json";
    performPostRequestAndAssertResponse(PRODUCT_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnPricingPriceListWhenRequested() throws Exception {
    String id = "AEE66281A08F42B6BC509B8A80A33C29";
    String jsonFile = "/jsons/pricingPriceList.read.json";
    performGetRequestAndAssertResponse(PRICINGPRICELIST_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnPricingPriceListWhenPosted() throws Exception {
    String jsonFile = "/jsons/pricingPriceList.write.json";
    String responseJsonFile = "/jsons/pricingPriceList.write.response.json";
    performPostRequestAndAssertResponse(PRICINGPRICELIST_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnPricingPriceListVersionWhenRequested() throws Exception {
    String id = "FDE536FE9D8C4B068C32CD6C3650B6B8";
    String jsonFile = "/jsons/pricingPriceListVersion.read.json";
    performGetRequestAndAssertResponse(PRICINGPRICELISTVERSION_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnPricingPriceListVersionWhenPosted() throws Exception {
    String jsonFile = "/jsons/pricingPriceListVersion.write.json";
    String responseJsonFile = "/jsons/pricingPriceListVersion.write.response.json";
    performPostRequestAndAssertResponse(PRICINGPRICELISTVERSION_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnProductPriceWhenRequested() throws Exception {
    String id = "41732EFCA6374148BFD8B08C8B12DB73";
    String jsonFile = "/jsons/productPrice.read.json";
    performGetRequestAndAssertResponse(PRODUCTPRICE_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnProductPriceWhenPosted() throws Exception {
    String jsonFile = "/jsons/productPrice.write.json";
    String responseJsonFile = "/jsons/productPrice.write.response.json";
    performPostRequestAndAssertResponse(PRODUCTPRICE_MODEL, jsonFile, responseJsonFile);
  }
}
