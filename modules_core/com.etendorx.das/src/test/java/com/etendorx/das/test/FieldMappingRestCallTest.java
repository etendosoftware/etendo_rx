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
import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "grpc.server.port=19090", "public-key=" + FieldMappingRestCallTest.publicKey })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
@ComponentScan("com.etendorx.das.test.projections")
public class FieldMappingRestCallTest {

  public static final String X_TOKEN = "X-TOKEN";
  public static final String AD_USER = "ADUser";
  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  @TestConfiguration
  @ComponentScan("com.etendorx.das.test.mappingbeans")
  static class FieldMappingRestCallTestConfiguration {
  }

  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjk3MTM1NjE4LCJhZF91c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiMCIsInNlYXJjaF9rZXkiOiIiLCJzZXJ2aWNlX2lkIjoiIn0.JXgTYhxyK23BZXJEOObs2n4ms4Fls3jXSsKNScT90j-22c8Ypo4ZYkjt0ucKLEjccuPSsxiyGfwH6fwtdoeAxSr9dQCQaV94jjJHXm75IgncgF1YHTUTCgZQ073prX6lyHdQZ0okhBrDMHiEo1_JWbLbEluiERzJFk0t9NYcXJcNAVvTK50zPJk4Ar0cwOEtIEP1nIPOeA8vQY2NBff5t3x_CCVNgdl3BC19nx-iWBf_6xyF0mRAs2uNQ5KI9aY63vjv_z1e_j4Wupff67oQHRwMLnJShemWnjbs71s6S5SbMrUfM2Z8TkLYC964rGlUYnmwHuCTkDoBDNKebYEINw";
  public static final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAocs6752BX8E9sUSkP0nnQlp9QNtTsBHB/jFZOro2ayCf203u3DHCPrLpLDZyrqAasIRRxKAAMNfmhl7/Hgg5FKeLp8rKEavlDTblVfVLvBmYpoJMxE2RumW4SdyP56LNnSlY49srflyiJyd9w+m0vVxMpXPT1RWTv+FJibVB8asqyUWW5sJgQ8Cr3PLI8KDCcwSpjlkkack3vB2ZiFtZVPntj4C6+/o5hcPgUeLVOFjH1H9zJP/ELLcueZtSbRo4J1CJsLUyY3ZCIk84wZwfielygT6Yl3tNqGGnxm7moXO8+y5uJZymoMqEhV5OnlolpAb/VuGZviv932fWzEMRjwIDAQAB";

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    TestcontainersUtils.setProperties(registry, postgreSQLContainer);
  }

  @Container
  public static final PostgreSQLContainer<?> postgreSQLContainer = TestcontainersUtils.createDBContainer();

  public static Stream<Arguments> validRequestParams() {
    return Stream.of(
        // Undefined username
        Arguments.of(AD_USER, "0", "System", 1, "System Administrator", true),
        Arguments.of(AD_USER, "100", "Admin", 13, "QA Testing Admin", false),
        Arguments.of(AD_USER, "167450A5BB284AB29C4FEF039E98C963", "F&B ES User", 4,
            "F&B España, S.A - Sales", false),
        Arguments.of(AD_USER, "20C5D31133D949F0BD25412DD1069612", " Rome", 0, null, false),
        Arguments.of(AD_USER, "26EF171A1D75485083D276D49AAACD45", "F&BESRSUser", 1, "F&BESRSUser",
            true),
        Arguments.of(AD_USER, "2748452130E84FF0B1A8292D88570F8F", "Joe Matt", 0, null, false),
        Arguments.of(AD_USER, "6628F632D484407CBCBD8E71C123A263", "Tom", 0, null, false),
        Arguments.of(AD_USER, "D249DE7A14FB4F77BC056A3738A63477", "F&BUSECUser", 1, "F&BUSECUser",
            true),
        Arguments.of(AD_USER, "E12DC7B3FF8C4F64924A98195223B1F8", "F&BUser", 1, "F&BUser", true),
        Arguments.of(AD_USER, "4028E6C72959682B01295F40C1D702E6", "John", 0, null, false),
        Arguments.of(AD_USER, "4028E6C72959682B01295F40C30F02EA", "Albert", 0, null, false));
  }

  @ParameterizedTest
  @MethodSource("validRequestParams")
  public void whenRestRead(String model, String id, String mappedName, int numRoles,
      String roleName, boolean isAdmin) throws Exception {
    var result = mockMvc.perform(
        get("/" + model + "/" + id + "?projection=mapping-test").header(X_TOKEN, TOKEN));
    result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    result.andExpect(MockMvcResultMatchers.jsonPath("$.name.mappedName").value(mappedName));
    result.andExpect(MockMvcResultMatchers.jsonPath("$.roles.length()").value(numRoles));
    if (numRoles > 0) {
      result.andExpect(
          MockMvcResultMatchers.jsonPath("$.roles[?(@.name == \"" + roleName + "\")]").exists());
      result.andExpect(MockMvcResultMatchers.jsonPath(
          "$.roles[?(@.name == \"" + roleName + "\" && @.isAdmin == " + BooleanUtils.toStringTrueFalse(
              isAdmin) + ")]").exists());
    }
  }
}
