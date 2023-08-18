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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19090")
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

  private static final String token = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjg2MDc2NjE2LCJhZF" +
      "91c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiMCIsInNlYXJjaF9rZXkiOiIi" +
      "LCJzZXJ2aWNlX2lkIjoiIn0.oBxwXw3Td0q1wNGVK4vSli4VGMGeRdfajwtzLCh9dVlLNFBFLJZ6EjJLUCFbZXTsxnwYHJfsHOQYcr7iWejdnP" +
      "Djy3l0CqGKFGxI-bNm_73Ky48fRdBakqzwFQExit9HfPDHd_iojp0hlpH736CWvh11v0QGja9Q0LdY4W69Np1waxUI2Qf4z2WfJaoQhIjdOq4B" +
      "cFoqqCBknVougK0J7ZMmxcOnSe6MSQ7UDzKgwunSSuT-iVeF4sxLb80hWu5dInfvn8iJVC8krJ9telWVqbo-dPoFbnFw9CtmTHpK153b4nj5U6" +
      "ZOTFP4kZqsqhvWo7wKg03O1emGmCKo1vg9Cg"; // NOSONAR

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
      Arguments.of(AD_USER, "167450A5BB284AB29C4FEF039E98C963", "F&B ES User", 4, "F&B España, S.A - Sales", false),
      Arguments.of(AD_USER, "20C5D31133D949F0BD25412DD1069612", " Rome", 0, null, false),
      Arguments.of(AD_USER, "26EF171A1D75485083D276D49AAACD45", "F&BESRSUser", 1, "F&BESRSUser", true),
      Arguments.of(AD_USER, "2748452130E84FF0B1A8292D88570F8F", "Joe Matt", 0, null, false),
      Arguments.of(AD_USER, "6628F632D484407CBCBD8E71C123A263", "Tom", 0, null, false),
      Arguments.of(AD_USER, "D249DE7A14FB4F77BC056A3738A63477", "F&BUSECUser", 1, "F&BUSECUser", true),
      Arguments.of(AD_USER, "E12DC7B3FF8C4F64924A98195223B1F8", "F&BUser", 1, "F&BUser", true),
      Arguments.of(AD_USER, "4028E6C72959682B01295F40C1D702E6", "John", 0, null, false),
      Arguments.of(AD_USER, "4028E6C72959682B01295F40C30F02EA", "Albert", 0, null, false)
    );
  }

  @ParameterizedTest
  @MethodSource("validRequestParams")
  public void whenRestRead(String model, String id, String mappedName, int numRoles, String roleName, boolean isAdmin) throws Exception {
    var result = mockMvc.perform(
      get("/" + model + "/" + id + "?projection=mapping-test").header(X_TOKEN, token)
    );
    result.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    result.andExpect(MockMvcResultMatchers.jsonPath("$.name.mappedName").value(mappedName));
    result.andExpect(MockMvcResultMatchers.jsonPath("$.roles.length()").value(numRoles));
    if(numRoles > 0) {
      result.andExpect(MockMvcResultMatchers.jsonPath("$.roles[?(@.name == \"" + roleName + "\")]").exists());
      result.andExpect(MockMvcResultMatchers.jsonPath("$.roles[?(@.name == \"" + roleName + "\" && @.isAdmin == " + BooleanUtils.toStringTrueFalse(isAdmin) +")]").exists());
    }
  }
}
