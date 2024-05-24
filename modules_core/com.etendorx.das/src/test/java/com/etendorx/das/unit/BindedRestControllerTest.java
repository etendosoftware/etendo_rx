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
package com.etendorx.das.unit;

import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.mapper.lib.BaseDTOModel;
import com.etendorx.entities.mapper.lib.BindedRestController;
import com.etendorx.entities.mapper.lib.JsonPathConverter;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * This class contains unit tests for the BindedRestController class.
 */
class BindedRestControllerTest {

  public static final String RAW_ENTITY = "rawEntity";

  /**
   * Mock classes for testing.
   */
  static class Car implements BaseSerializableObject {
    public String getId() {
      return "id";
    }

    @Override
    public String get_identifier() {
      return "_id";
    }

    @Override
    public String getTableId() {
      throw new UnsupportedOperationException("Table ID is not set");
    }
  }

  static class CarDTORead implements BaseDTOModel {
    public String getId() {
      return "id";
    }

    @Override
    public void setId(String id) {
      // do nothing
    }
  }

  static class CarDTOWrite implements BaseDTOModel {
    public String getId() {
      return "id";
    }

    @Override
    public void setId(String id) {
      // do nothing
    }
  }

  /**
   * Mock objects for testing.
   */
  @Mock
  private JsonPathConverter<CarDTOWrite> converter;

  @Mock
  private BaseDTORepositoryDefault<Car, CarDTORead, CarDTOWrite> repository;

  @Mock
  private Validator validator;

  private BindedRestController<CarDTORead, CarDTOWrite> controller;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new BindedRestController<>(converter, repository, validator) {
    };
  }

  /**
   * Test the findAll method.
   */
  @Test
  void findAllShouldReturnPage() {
    // Mock setup
    Page<CarDTORead> expectedPage = mock(Page.class);
    given(repository.findAll(any())).willReturn(expectedPage);

    // Execute
    Page<CarDTORead> result = controller.findAll(Pageable.unpaged());

    // Assert
    assertEquals(expectedPage, result);
  }

  /**
   * Test the get method with a valid ID.
   */
  @Test
  void getShouldReturnEntity() {
    // Mock setup
    CarDTORead expectedEntity = mock(CarDTORead.class);
    given(repository.findById(anyString())).willReturn(expectedEntity);

    // Execute
    ResponseEntity<CarDTORead> response = controller.get("someId");

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedEntity, response.getBody());
  }

  /**
   * Test the get method with an invalid ID.
   */
  @Test
  void getShouldReturnNotFound() {
    // Mock setup
    given(repository.findById(anyString())).willReturn(null);

    // Assert
    assertThrows(ResponseStatusException.class, () -> controller.get("invalidId"));
  }

  /**
   * Test the post method.
   */
  @Test
  void postShouldCreateEntity() {
    // Mock setup
    CarDTOWrite dtoEntity = mock(CarDTOWrite.class);
    CarDTORead savedEntity = mock(CarDTORead.class);
    given(converter.convert(anyString())).willReturn(dtoEntity);
    given(repository.save(any())).willReturn(savedEntity);

    // Execute
    ResponseEntity<Object> response = controller.post(RAW_ENTITY, null);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(savedEntity, response.getBody());
  }

  /**
   * Test the put method with a valid ID.
   */
  @Test
  void putShouldUpdateEntity() {
    // Mock setup
    String id = "validId";
    CarDTOWrite dtoEntity = mock(CarDTOWrite.class);
    CarDTORead updatedEntity = mock(CarDTORead.class);
    given(converter.convert(anyString())).willReturn(dtoEntity);
    given(repository.update(any())).willReturn(updatedEntity);

    // Execute
    ResponseEntity<CarDTORead> response = controller.put(id, RAW_ENTITY);

    // Assert
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(updatedEntity, response.getBody());
  }

  /**
   * Test the put method with an invalid ID.
   */
  @Test
  void putShouldFailWithBadRequestOnInvalidId() {
    // Assert
    assertThrows(ResponseStatusException.class, () -> controller.put(null, RAW_ENTITY));
  }
}
