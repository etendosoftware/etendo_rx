/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.entities.mapper.lib;

import com.etendorx.das.EtendorxDasApplication;
import com.etendorx.das.externalid.ExternalIdServiceImpl;
import com.etendorx.das.test.RepositoryTest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * This class contains unit tests for the JsonPathEntityRetrieverBase class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "grpc.server.port=19091", "public-key=" + RepositoryTest.publicKey },
    classes = EtendorxDasApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration()
@AutoConfigureMockMvc
@ComponentScan({"com.etendorx.das.test.projections","com.etendorx.das.externalid"})
public class JsonPathEntityRetrieverBaseTests {

  /**
   * Mock object for testing.
   */
  @Mock
  private JpaSpecificationExecutor<Car> repository;

  @Mock
  private ExternalIdServiceImpl externalIdService;
  /**
   * Object under test.
   */
  private JsonPathEntityRetrieverBase<Car> retriever;

  /**
   * Test data classes.
   */
  @Data
  @AllArgsConstructor
  static class Car {
    String id;
    String name;
  }

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(externalIdService.getExternalId(any(), any(), any())).thenReturn("1");
    retriever = new JsonPathEntityRetrieverBase<>() {
      @Override
      protected String getTableId() {
        return "";
      }

      @Override
      protected ExternalIdService getExternalIdService() {
        return externalIdService;
      }

      @Override
      public JpaSpecificationExecutor<Car> getRepository() {
        return repository;
      }

      @Override
      public String[] getKeys() {
        return new String[] { "id" };
      }
    };
  }

  /**
   * Test the get method with a valid key.
   * The method should return the expected entity.
   */
  @Test
  void getWithValidKeyShouldReturnEntity() {
    // Given
    Car expectedEntity = new Car("1", "Car 1");
    List<Car> list = Collections.singletonList(expectedEntity);
    String key = "1";
    when(repository.findAll(any(Specification.class))).thenReturn(
        list);

    // When
    Car result = retriever.get(key);

    // Then
    assertNotNull(result);
    assertEquals(expectedEntity, result);
  }

  /**
   * Test the get method with a null key.
   * The method should return null.
   */
  @Test
  void getWithNullKeyShouldReturnNull() {
    // When
    Car result = retriever.get((Object) null);

    // Then
    assertNull(result);
  }

  @Test
  void getWithIntegerKeyShouldReturnObject() {
    // Given
    Car expectedEntity = new Car("1", "Car 1");
    List<Car> list = Collections.singletonList(expectedEntity);
    Integer key = 1;
    when(repository.findAll(any(Specification.class))).thenReturn(list);

    // When
    Car result = retriever.get(key);

    // Then
    assertNotNull(result);
    assertEquals(expectedEntity, result);

  }

  /**
   * Test the get method with a mismatched key size.
   * The method should throw an IllegalArgumentException.
   */
  @Test
  void getWithMismatchedKeySizeShouldThrowException() {
    // Given
    var keySet = new TreeSet<String>();
    keySet.add("1");
    keySet.add("2"); // Adding extra to create size mismatch

    // Then
    assertThrows(IllegalArgumentException.class, () -> retriever.get(keySet));
  }

  /**
   * Test the get method with an unsupported id type.
   * The method should throw a ResponseStatusException.
   */
  @Test
  void getWithUnsupportedIdTypeShouldThrowException() {
    // Given
    Object unsupportedKey = new Object();

    // Then
    assertThrows(ResponseStatusException.class, () -> retriever.get(unsupportedKey));
  }
}
