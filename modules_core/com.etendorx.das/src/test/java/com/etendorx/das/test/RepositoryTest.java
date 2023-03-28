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

import static com.etendorx.utils.auth.key.context.FilterContext.setUserContextFromToken;

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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.etendorx.entities.jparepo.ADUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "grpc.server.port=19091")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration
@AutoConfigureMockMvc
public class RepositoryTest {
  public static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNjgwMTExOTc2LCJhZF91" +
      "c2VyX2lkIjoiMTAwIiwiYWRfY2xpZW50X2lkIjoiMCIsImFkX29yZ19pZCI6IjAifQ.b7-ooaDHbPvyOlT-1eZ3cKlhaSOuhHAoEv6eHElpNeSKR" +
      "dxZHgeiCSCc5mO-FhEygJhtPWhCOQvqGzDTBqPx8pKp32NoyLhiSHIuI13WZMnkW6r7pcbkmTqZ7xocktHvjQfIf6s3nxK0bIc5NG8aQzhrR-6Un" +
      "FIuF3k5OYspQVKqX0etld5nJ0W126c2ZqXXScNAGSshFulEhyiK7WvuJ0ciRE6lHf_qRA2Etv67SXfStIgprbT5mcpyJv8HZFatlU88_AdWh7CaC" +
      "4RdqEmx46TRQJHTKTU8Pl7LqLDY9dGNFBDeov2Wajuu6q5VMS6F_cG95q2AxsZ-3Cw9BM7CWA";

  @Autowired
  private ADUserRepository userRepository;
  @Autowired
  private TestRestTemplate testRestTemplate;
  @Autowired
  private MockMvc mockMvc;

  @Test
  public void whenReadUser() {
    setUserContextFromToken(TOKEN);
    var allUsers = userRepository.findAll();
    assert allUsers.iterator().hasNext();
    var userRetrieved = allUsers.iterator().next();
    assert userRetrieved.getId() != null;
  }

  @Test
  public void whenFindByName() {
    setUserContextFromToken(TOKEN);
    var userList = userRepository.searchByUsername("admin", true, null);
    assert userList.getSize() == 1;
    assert userList.getContent().get(0) != null;
  }

  @Test
  public void generateCsvFileForQueryIsOkWhenDefaultFiltersIsApplyWithCsvParameterTest() throws Exception {
    String endpoint = "http://localhost:8092/";
    List<String> resultUrls = extractHrefsFromEtendoPath(endpoint);
    List<String> cleanUrls = new ArrayList<>();
    for (String url : resultUrls) {
      cleanUrls.add(cleanUrl(url));
    }
    FileWriter writer = new FileWriter("src/test/resources/urlData.csv");
    Path path = Paths.get("src/test/resources/urlData.csv");
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
  public void queryIsOkWhenDefaultFiltersIsApplyWithCsvParameter(String parametrizedUrl) throws IOException, InterruptedException {
    //URL with bugs in ETENDO
    List<String> excludeUrls = new ArrayList<>(Arrays.asList(
        "http://localhost:8092/OrganizationModuleV",
        "http://localhost:8092/OBSCHEDSimpropTriggers",
        "http://localhost:8092/OBSCHEDSchedulerState",
        "http://localhost:8092/ADOrgModule",
        "http://localhost:8092/OBSCHEDPausedTriggerGrps",
        "http://localhost:8092/OBSCHEDSimpleTriggers",
        "http://localhost:8092/ADModule",
        "http://localhost:8092/OBSCHEDFiredTriggers",
        "http://localhost:8092/OBSCHEDCalendars",
        "http://localhost:8092/OBSCHEDBlobTriggers",
        "http://localhost:8092/SQLScript",
        "http://localhost:8092/ProcessPlanTotalized",
        "http://localhost:8092/ADEPInstancePara",
        "http://localhost:8092/OBSCHEDLocks",
        "http://localhost:8092/ManufacturingCostc"));

    if (!excludeUrls.contains(parametrizedUrl)) {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(parametrizedUrl))
          .GET()
          .header("X-TOKEN", TOKEN)
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      Assertions.assertEquals(200, response.statusCode());
    } else {
      Assertions.assertEquals(true, excludeUrls.contains(parametrizedUrl));
    }
  }


  public List<String> extractHrefsFromEtendoPath(String endpointUrl) throws JsonProcessingException {
    RestTemplate restTemplate = new RestTemplate();
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
