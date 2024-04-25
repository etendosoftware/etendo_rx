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

import com.etendorx.entities.entities.BaseDASRepository;
import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.mapper.lib.BaseDTOModel;
import com.etendorx.entities.mapper.lib.DTOConverter;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the BaseDTORepositoryDefault class.
 */
class BaseDTORepositoryDefaultTests {

  // Mock objects for testing
  @Mock
  Car car;
  @Mock
  CarDTO carDTORead;
  @Mock
  CarDTO carDTOWrite;

  @Mock
  BaseDTOModel baseDTOModel;

  @Mock
  RestCallTransactionHandler transactionHandler;

  @Mock
  DTOConverter<Car, CarDTO, CarDTO> converter;
  @Mock
  JsonPathEntityRetriever<Car> retriever;
  @Mock
  Validator validator;
  @Mock
  BaseDASRepository<Car> repository;

  @InjectMocks
  BaseDTORepositoryDefault<Car, CarDTO, CarDTO> baseDTORepositoryDefault;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * A mock Car class for testing.
   */
  static class Car implements BaseSerializableObject {
    public String getId() {
      return "id";
    }

    @Override
    public String get_identifier() {
      return "_id";
    }
  }

  /**
   * A mock CarDTO class for testing.
   */
  static class CarDTO implements BaseDTOModel {
    public String getId() {
      return "id";
    }

    @Override
    public void setId(String id) {
      // do nothing
    }
  }

  /**
   * Test the save method when valid data is provided.
   */
  @Test
  void shouldSaveWhenValidDataProvided() {
    // Arrange
    final String NEW_ID = "ABDC";
    Car carBeforeSave = mock(Car.class);
    Car carAfterSave = mock(Car.class);
    CarDTO carDTOBeforeSave = mock(CarDTO.class);
    CarDTO carDTOAfterSave = mock(CarDTO.class);
    when(carDTOBeforeSave.getId()).thenReturn(null);
    when(carDTOAfterSave.getId()).thenReturn(NEW_ID);
    //
    when(converter.convert(any(), any())).thenReturn(carBeforeSave);
    when(converter.convert(carBeforeSave)).thenReturn(carDTOBeforeSave);
    when(converter.convert(any(Car.class))).thenReturn(carDTOAfterSave);
    when(converter.convert(any(), any())).thenReturn(carBeforeSave);
    when(converter.convert(carAfterSave)).thenReturn(carDTOAfterSave);
    //
    when(retriever.get(anyString())).thenReturn(carAfterSave);
    //
    when(validator.validate(carBeforeSave)).thenReturn(Set.of());
    Set<ConstraintViolation<Car>> violations = mock(Set.class);
    when(validator.validate(carBeforeSave)).thenReturn(violations);
    when(violations.isEmpty()).thenReturn(true);
    //
    when(repository.save(any())).thenReturn(carAfterSave);

    // Assuming repository.save() should be called, mock its behavior as well
    when(repository.save(any())).thenReturn(carAfterSave);

    // Act
    CarDTO result = baseDTORepositoryDefault.save(carDTORead);

    // Assert
    // Verify that the converter's convert method was called as expected
    verify(converter, times(1)).convert(carDTORead, null);
    verify(converter, times(2)).convert(carAfterSave);
    verify(validator, times(1)).validate(carBeforeSave);
    verify(repository, times(1)).save(carBeforeSave);

    // Optionally, assert the result of the save operation
    assertNotNull(result, "The result should not be null.");
    assertEquals(carDTOAfterSave, result, "The returned DTO should match the expected value.");

    // You might also want to verify that the transactionHandler.begin() and transactionHandler.commit() methods were called
    verify(transactionHandler, times(1)).begin();
    verify(transactionHandler, times(1)).commit();

    // Additional Verifications
    // Verify no unwanted interactions
    verifyNoMoreInteractions(transactionHandler, repository, converter, validator);

  }

}
