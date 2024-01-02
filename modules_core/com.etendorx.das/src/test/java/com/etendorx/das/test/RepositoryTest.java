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
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.FilterContext;
import com.etendorx.utils.auth.key.context.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.etendorx.utils.auth.key.context.FilterContext.setUserContextFromToken;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "grpc.server.port=19091", "public-key=" + RepositoryTest.publicKey })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class RepositoryTest {
  private static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjk3MTM1NjE4LCJhZF91c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAiLCJhZF9yb2xlX2lkIjoiMCIsInNlYXJjaF9rZXkiOiIiLCJzZXJ2aWNlX2lkIjoiIn0.JXgTYhxyK23BZXJEOObs2n4ms4Fls3jXSsKNScT90j-22c8Ypo4ZYkjt0ucKLEjccuPSsxiyGfwH6fwtdoeAxSr9dQCQaV94jjJHXm75IgncgF1YHTUTCgZQ073prX6lyHdQZ0okhBrDMHiEo1_JWbLbEluiERzJFk0t9NYcXJcNAVvTK50zPJk4Ar0cwOEtIEP1nIPOeA8vQY2NBff5t3x_CCVNgdl3BC19nx-iWBf_6xyF0mRAs2uNQ5KI9aY63vjv_z1e_j4Wupff67oQHRwMLnJShemWnjbs71s6S5SbMrUfM2Z8TkLYC964rGlUYnmwHuCTkDoBDNKebYEINw";
  public static final String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAocs6752BX8E9sUSkP0nnQlp9QNtTsBHB/jFZOro2ayCf203u3DHCPrLpLDZyrqAasIRRxKAAMNfmhl7/Hgg5FKeLp8rKEavlDTblVfVLvBmYpoJMxE2RumW4SdyP56LNnSlY49srflyiJyd9w+m0vVxMpXPT1RWTv+FJibVB8asqyUWW5sJgQ8Cr3PLI8KDCcwSpjlkkack3vB2ZiFtZVPntj4C6+/o5hcPgUeLVOFjH1H9zJP/ELLcueZtSbRo4J1CJsLUyY3ZCIk84wZwfielygT6Yl3tNqGGnxm7moXO8+y5uJZymoMqEhV5OnlolpAb/VuGZviv932fWzEMRjwIDAQAB";

  @LocalServerPort
  private int port;
  @Autowired
  private HttpServletRequest httpServletRequest;
  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  @org.springframework.boot.test.context.TestConfiguration
  @ComponentScan("com.etendorx.das.test.projections")
  static class RepositoryTestConfiguration {
    @Bean
    public UserContext userContext() {
      return new UserContext();
    }
  }

  @Autowired
  private UserContext userContext;

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    TestcontainersUtils.setProperties(registry, postgreSQLContainer);
  }

  @Container
  public static final PostgreSQLContainer<?> postgreSQLContainer = TestcontainersUtils.createDBContainer();

  @Test
  public void whenReadUser() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    setUserContextFromToken(userContext, publicKey, null, TOKEN, request);
    AppContext.setCurrentUser(userContext);
    var allUsers = userRepository.findAll();
    assert allUsers.iterator().hasNext();
    var userRetrieved = allUsers.iterator().next();
    assert userRetrieved.getId() != null;
  }

  @Test
  public void whenFindByName() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter(FilterContext.NO_ACTIVE_FILTER_PARAMETER, FilterContext.TRUE);
    request.setMethod("GET");
    UserContext userContext = new UserContext();
    setUserContextFromToken(userContext, publicKey, null, TOKEN, request);
    var userList = userRepository.searchByUsername("admin", null);
    assert userList.getSize() == 1;
    assert userList.getContent().get(0) != null;
  }

  @Test
  public void generateCsvFileForQueryIsOkWhenDefaultFiltersIsApplyWithCsvParameterTest()
      throws Exception {
    String endpoint = "http://localhost:" + port + "/";
    List<String> resultUrls = extractHrefsFromEtendoPath(endpoint);
    List<String> cleanUrls = new ArrayList<>();
    for (String url : resultUrls) {
      cleanUrls.add(cleanUrl(url));
    }

    Path tmpDir = Files.createTempDirectory("csv");
    FileWriter writer = new FileWriter(tmpDir + "/urlData.csv");
    Path path = Paths.get(tmpDir + "/urlData.csv");
    try {
      String collect = cleanUrls.stream().collect(Collectors.joining("\n"));
      writer.append("parametrizedUrl");
      writer.append("\n");
      writer.write(collect);
    } catch (Exception e) {
      throw new Exception("writer will be close");
    } finally {
      writer.close();
    }

    Assertions.assertTrue(Files.exists(path));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/urlData.csv", numLinesToSkip = 1)
  public void queryIsOkWhenDefaultFiltersIsApplyWithCsvParameter(String parametrizedUrl)
      throws IOException, InterruptedException {

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(parametrizedUrl.replace("8092", String.valueOf(port))))
        .GET()
        .header("X-TOKEN", TOKEN)
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Assertions.assertEquals(200, response.statusCode());

  }

  public List<String> extractHrefsFromEtendoPath(String endpointUrl)
      throws JsonProcessingException {
    RestTemplate restTemplate = new RestTemplateBuilder(rt -> {
      rt.getInterceptors().add(((request, body, execution) -> {
        request.getHeaders().add("X-TOKEN", TOKEN);
        return execution.execute(request, body);
      }));
    }).build();
    URI uri = UriComponentsBuilder.fromHttpUrl(endpointUrl).build().encode().toUri();
    String response = restTemplate.getForObject(uri, String.class);
    return extractHrefsFromResponse(response);
  }

  private List<String> extractHrefsFromResponse(String json) throws JsonProcessingException {
    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new ObjectMapper();
    com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(json);
    JsonNode linksNode = rootNode.path("_links");
    List<String> hrefValues = new ArrayList<>();

    for (JsonNode node : linksNode) {
      if (node.isArray()) {
        for (JsonNode arrayNode : node) {
          JsonNode hrefNode = arrayNode.path("href");
          if (!hrefNode.isMissingNode()) {
            hrefValues.add(hrefNode.asText());
          }
        }
      } else {
        JsonNode hrefNode = node.path("href");
        if (!hrefNode.isMissingNode()) {
          hrefValues.add(hrefNode.asText());
        }
      }
    }

    return hrefValues;
  }

  private static String cleanUrl(String url) {
    String regex = "\\{([^}]+)\\}";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(url);
    return matcher.replaceAll("");
  }

}
