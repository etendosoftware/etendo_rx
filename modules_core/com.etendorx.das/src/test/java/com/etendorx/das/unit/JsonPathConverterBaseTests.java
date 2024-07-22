/*
 * Copyright 2022-2023  Futit Services SL
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
package com.etendorx.das.unit;

import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.ReturnKey;
import com.jayway.jsonpath.DocumentContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This class contains unit tests for the JsonPathConverterBase class.
 */
public class JsonPathConverterBaseTests {

  /**
   * Mock object for testing.
   */
  @Mock
  private DocumentContext documentContext;

  /**
   * Object under test.
   */
  private JsonPathConverterBase<Car> jsonPathConverterBase;

  /**
   * Test data classes.
   */
  @Data
  @AllArgsConstructor
  static class Car {
    String id;
    String name;
  }

  static class CarDTORead {
    String id;
    String name;
  }

  static class CarDTOWrite {
    String id;
    String name;
  }

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    jsonPathConverterBase = new JsonPathConverterBase<>() {
      @Override
      public Car convert(String rawData) {
        return null;
      }
    };
  }

  /**
   * Test the read method with a valid path.
   * The method should return a ReturnKey with the expected value and no error.
   */
  @Test
  void shouldReadValueSuccessfully() {
    String path = "$.car";
    Car expectedCar = new Car("1", "Tesla");
    when(documentContext.read(path, Car.class)).thenReturn(expectedCar);

    ReturnKey<Car> result = jsonPathConverterBase.read(documentContext, path, Car.class);

    assertNotNull(result);
    assertEquals(path, result.getPath());
    assertEquals(expectedCar, result.getValue());
    assertFalse(result.isNullValue());
    assertFalse(result.isError());

    verify(documentContext, times(1)).read(path, Car.class);
  }

  /**
   * Test the read method with a path that does not exist in the DocumentContext.
   * The method should return a ReturnKey with a null value and no error.
   */
  @Test
  void shouldHandleNotFoundPath() {
    String path = "$.missingCar";
    when(documentContext.read(path, Car.class)).thenThrow(new RuntimeException("Path not found"));

    ReturnKey<Car> result = jsonPathConverterBase.read(documentContext, path, Car.class);

    assertNotNull(result);
    assertEquals(path, result.getPath());
    assertNull(result.getValue());
    assertFalse(result.isError()); // Not an error if path not found
    assertTrue(result.isNullValue());

    verify(documentContext, times(1)).read(path, Car.class);
  }

}
