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
import org.openbravo.model.common.uom.UOM;
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "grpc.server.port=19090", "scan.basePackage=com.etendorx.integration.to_openbravo",
    "public-key=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAocs6752BX8E9sUSkP0nnQlp9QNtTsBHB/jFZOro2ayCf203u3DHCPrLpLDZyrqAasIRRxKAAMNfmhl7/Hgg5FKeLp8rKEavlDTblVfVLvBmYpoJMxE2RumW4SdyP56LNnSlY49srflyiJyd9w+m0vVxMpXPT1RWTv+FJibVB8asqyUWW5sJgQ8Cr3PLI8KDCcwSpjlkkack3vB2ZiFtZVPntj4C6+/o5hcPgUeLVOFjH1H9zJP/ELLcueZtSbRo4J1CJsLUyY3ZCIk84wZwfielygT6Yl3tNqGGnxm7moXO8+y5uJZymoMqEhV5OnlolpAb/VuGZviv932fWzEMRjwIDAQAB" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class MappingsRestTest {

  public static final String X_TOKEN = "X-TOKEN";
  private static final String BASE_URL = "/obmap/";
  private static final String DATE_FORMAT = "?_dateFormat=yyyy-MM-dd HH:mm:ss";
  private static final String TIME_ZONE = "&_timeZone=GMT-3";
  private static final String ACCEPT_HEADER = "application/json; charset=UTF-8";
  private static final String ADCLIENT_MODEL = "ADClient";
  private static final String ADUSER_MODEL = "ADUser";
  private static final String BUSINESSPARTNER_MODEL = "BusinessPartner";
  private static final String BPCATEGORY_MODEL = "BusinessPartnerCategory";
  private static final String BPLOCATION_MODEL = "BusinessPartnerLocation";
  private static final String BPTAXCATEGORY_MODEL = "BusinessPartnerTaxCategory";
  private static final String COUNTRY_MODEL = "Country";
  private static final String CURRENCY_MODEL = "Currency";
  private static final String DOCUMENTTYPE_MODEL = "DocumentType";
  private static final String FINANCIALACCOUNT_MODEL = "FIN_Financial_Account";
  private static final String PAYMENTMETHOD_MODEL = "FIN_PaymentMethod";
  private static final String PAYMENTTERM_MODEL = "FinancialMgmtPaymentTerm";
  private static final String GREETING_MODEL = "Greeting";
  private static final String INVOICE_MODEL = "Invoice";
  private static final String LOCATION_MODEL = "Location";
  private static final String LOCATOR_MODEL = "Locator";
  private static final String ORDER_MODEL = "Order";
  private static final String ORGANIZATION_MODEL = "Organization";
  private static final String PRICINGPRICELIST_MODEL = "PricingPriceList";
  private static final String PRICINGPRICELISTVERSION_MODEL = "PricingPriceListVersion";
  private static final String PRODUCT_MODEL = "Product";
  private static final String PRODUCTCATEGORY_MODEL = "ProductCategory";
  private static final String PRODUCTPRICE_MODEL = "ProductPrice";
  private static final String REGION_MODEL = "Region";
  private static final String TAXCATEGORY_MODEL = "FinancialMgmtTaxCategory";
  private static final String TAXRATE_MODEL = "FinancialMgmtTaxRate";
  private static final String UOM_MODEL = "UOM";
  private static final String WAREHOUSE_MODEL = "Warehouse";

  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNzA1NDEzNDk3LCJhZF91c2VyX2lkIjoiQTUzMEFBRTIyQzg2NDcwMkI3RTFDMjJENThFN0IxN0IiLCJhZF9jbGllbnRfaWQiOiIyM0M1OTU3NUI5Q0Y0NjdDOTYyMDc2MEVCMjU1QjM4OSIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiNDJEMEVFQjFDNjZGNDk3QTkwREQ1MjZEQzU5N0U2RjAiLCJzZWFyY2hfa2V5IjoiT0JDb25uZWN0b3IiLCJzZXJ2aWNlX2lkIjoiMDMyRDY4QjI1NDg4NENCOThCRTg4Q0VCRjRGN0RGNEIifQ.S8vWemHh6IysmysVJW6Ighy0Pw8ROiKdyDJM0mpiBw6bDA12Xg1qhsjSuBiy7T20V4e0p22yiHZPeqbp2U_s_URR0665uktHXocSE_UAVGymyK0eEGwmOuwg1OzTLVXNEKa5Vi0wwlia6yyyb8H1VQYyVXrM5cmqptcqDiKm-ZeoG1W2jxFpFS5abeJBIFMKLBDV6Zcvwc9Q2WG9M70l7OC169eJgYxf8bvk_gCnVxb9PbIFRcJiCmaSLvvGZcfIFj6WJZ5twSxnPQ-fPsEWptv_Hv2HSIKJgKzyyeG4XViQYBlQOJhed5CYWGq6_nLb3Rb9OFT5lPvhH8BmvAHRaw";

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

  private void performGetRequestAndAssertResponse(String model, String id, String jsonFile)
      throws Exception {
    String expectedJsonData = TestcontainersUtils.getJsonFromFile(jsonFile);
    var result = mockMvc.perform(
            get(BASE_URL + model + "/" + id + DATE_FORMAT + TIME_ZONE).header(X_TOKEN, TOKEN)
                .header("Accept", ACCEPT_HEADER)
                .characterEncoding(UTF_8.toString()))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andReturn();
    assertResponse(result, expectedJsonData);
  }

  private void performPostRequestAndAssertResponse(String model, String jsonFile,
      String responseJsonFile) throws Exception {
    String postedJsonData = TestcontainersUtils.getJsonFromFile(jsonFile);
    String expectedJsonData = TestcontainersUtils.getJsonFromFile(responseJsonFile);
    var result = mockMvc.perform(
            post(BASE_URL + model + DATE_FORMAT + TIME_ZONE + "&_triggerEnabled=false").header(X_TOKEN,
                TOKEN).header("Accept", ACCEPT_HEADER).content(postedJsonData))
        .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
        .andReturn();
    assertResponse(result, expectedJsonData);
  }

  private void assertResponse(MvcResult result, String expectedJsonData) throws Exception {
    String actualJsonData = result.getResponse().getContentAsString();
    assertEquals(expectedJsonData, actualJsonData, JSONCompareMode.LENIENT);
  }

  @Test
  void shouldReturnADClientWhenRequested() throws Exception {
    String id = "23C59575B9CF467C9620760EB255B389";
    String jsonFile = "/jsons/adClient.read.json";
    performGetRequestAndAssertResponse(ADCLIENT_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnADUserWhenRequested() throws Exception {
    String id = "A530AAE22C864702B7E1C22D58E7B17B";
    String jsonFile = "/jsons/adUser.read.json";
    performGetRequestAndAssertResponse(ADUSER_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnBPartnerWhenRequested() throws Exception {
    String id = "A6750F0D15334FB890C254369AC750A8";
    String jsonFile = "/jsons/bPartner.read.json";
    performGetRequestAndAssertResponse(BUSINESSPARTNER_MODEL, id, jsonFile);
  }

  @Test
  void shouldCreateBPartnerhenPosted() throws Exception {
    String jsonFile = "/jsons/bPartner.write.json";
    String responseJsonFile = "/jsons/bPartner.write.response.json";
    performPostRequestAndAssertResponse(BUSINESSPARTNER_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnBPCategoryWhenRequested() throws Exception {
    String id = "5C9FA34263294CA694EACB9C20FB115C";
    String jsonFile = "/jsons/bpCategory.read.json";
    performGetRequestAndAssertResponse(BPCATEGORY_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnBPLocationWhenRequested() throws Exception {
    String id = "BFE1FB707BA84A6D8AF61A785F3CE1C1";
    String jsonFile = "/jsons/bpLocation.read.json";
    performGetRequestAndAssertResponse(BPLOCATION_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnBPTaxCategoryWhenRequested() throws Exception {
    String id = "742AF058A304496C85A4DA39E4E35423";
    String jsonFile = "/jsons/bpTaxCategory.read.json";
    performGetRequestAndAssertResponse(BPTAXCATEGORY_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnCountryWhenRequested() throws Exception {
    String id = "106";
    String jsonFile = "/jsons/country.read.json";
    performGetRequestAndAssertResponse(COUNTRY_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnCurrencyWhenRequested() throws Exception {
    String id = "102";
    String jsonFile = "/jsons/currency.read.json";
    performGetRequestAndAssertResponse(CURRENCY_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnDocumentTypeWhenRequested() throws Exception {
    String id = "D00B3241E3D14D83A48157DEF6BB58FE";
    String jsonFile = "/jsons/documentType.read.json";
    performGetRequestAndAssertResponse(DOCUMENTTYPE_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnFinancialAccountWhenRequested() throws Exception {
    String id = "2C059762760F4F3D8A91342ED988DEB8";
    String jsonFile = "/jsons/financialAccount.read.json";
    performGetRequestAndAssertResponse(FINANCIALACCOUNT_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnPaymentMethodWhenRequested() throws Exception {
    String id = "47506D4260BA4996B92768FF609E6665";
    String jsonFile = "/jsons/paymentMethod.read.json";
    performGetRequestAndAssertResponse(PAYMENTMETHOD_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnPaymentTermWhenRequested() throws Exception {
    String id = "A8EB69EF071A43DDBFF1A796B59E5B1D";
    String jsonFile = "/jsons/paymentTerm.read.json";
    performGetRequestAndAssertResponse(PAYMENTTERM_MODEL, id, jsonFile);
  }

  @Test
  void shouldCreateGreetingWhenPosted() throws Exception {
    String jsonFile = "/jsons/greeting.write.json";
    String responseJsonFile = "/jsons/greeting.write.response.json";
    performPostRequestAndAssertResponse(GREETING_MODEL, jsonFile, responseJsonFile);
  }

  @Test
  void shouldReturnProductCategoryWhenRequested() throws Exception {
    String id = "DC7F246D248B4C54BFC5744D5C27704F";
    String jsonFile = "/jsons/productCategory.read.json";
    performGetRequestAndAssertResponse(PRODUCTCATEGORY_MODEL, id, jsonFile);
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
  void shouldReturnLocationWhenRequested() throws Exception {
    String id = "3E18625345CD4D27915FEDCE344FF79F";
    String jsonFile = "/jsons/location.read.json";
    performGetRequestAndAssertResponse(LOCATION_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnLocatorWhenRequested() throws Exception {
    String id = "54EB861A446D464EAA433477A1D867A6";
    String jsonFile = "/jsons/locator.read.json";
    performGetRequestAndAssertResponse(LOCATOR_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnOrderWhenRequested() throws Exception {
    String id = "0AC230C0DDA4435A949B40602A183F45";
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
  void shouldReturnOrganizationWhenRequested() throws Exception {
    String id = "0";
    String jsonFile = "/jsons/organization.read.json";
    performGetRequestAndAssertResponse(ORGANIZATION_MODEL, id, jsonFile);
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

  @Test
  void shouldReturnRegionWhenRequested() throws Exception {
    String id = "A44A7F2FCFD34CC78DC5546B067CD816";
    String jsonFile = "/jsons/region.read.json";
    performGetRequestAndAssertResponse(REGION_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnTaxCategoryWhenRequested() throws Exception {
    String id = "57B9430EE6DA49EEBEF1AC05B8B4A54C";
    String jsonFile = "/jsons/taxCategory.read.json";
    performGetRequestAndAssertResponse(TAXCATEGORY_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnTaxRateWhenRequested() throws Exception {
    String id = "4BF9470755AD4395AABCB77F5014CBE8";
    String jsonFile = "/jsons/taxRate.read.json";
    performGetRequestAndAssertResponse(TAXRATE_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnUOMWhenRequested() throws Exception {
    String id = "EA";
    String jsonFile = "/jsons/uom.read.json";
    performGetRequestAndAssertResponse(UOM_MODEL, id, jsonFile);
  }

  @Test
  void shouldReturnWarehouseWhenRequested() throws Exception {
    String id = "B2D40D8A5D644DD89E329DC297309055";
    String jsonFile = "/jsons/warehouse.read.json";
    performGetRequestAndAssertResponse(WAREHOUSE_MODEL, id, jsonFile);
  }

}
