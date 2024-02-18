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
package com.etendorx.das.unit;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendorx.das.utils.MappingUtilsImpl;
import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.jparepo.ETRX_Constant_ValueRepository;
import com.etendorx.utils.auth.key.context.AppContext;
import com.etendorx.utils.auth.key.context.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MappingUtilsImplTest {

  @Mock
  private ETRX_Constant_ValueRepository constantValueRepository;

  private MappingUtilsImpl mappingUtils;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mappingUtils = new MappingUtilsImpl(constantValueRepository);
    // Setup AppContext mock here if possible
    var uc = new UserContext();
    uc.setDateFormat("yyyy-MM-dd");
    uc.setTimeZone("UTC");
    AppContext.setCurrentUser(uc);
  }

  @Test
  void testHandleBaseObjectWithSerializableObject() {
    // Given
    BaseSerializableObject serializableObject = mock(BaseSerializableObject.class);
    when(serializableObject.get_identifier()).thenReturn("123");

    // When
    Object result = mappingUtils.handleBaseObject(serializableObject);

    // Then
    assertEquals("123", result);
  }

  @Test
  void testHandleBaseObjectWithTimestamp() {
    // Assume AppContext is set to a specific dateFormat and timeZone
    Timestamp timestamp = new Timestamp(new Date().getTime());
    // Use a fixed date and time for testing, and configure AppContext accordingly

    // When
    Object result = mappingUtils.handleBaseObject(timestamp);

    // Then
    assertNotNull(result);
    // Verify the formatted date string according to the fixed date/time and AppContext settings
  }

  @Test
  void testParseDate() {
    // Given
    String inputDate = "2023-01-01";
    // Assume AppContext is set to a specific dateFormat

    // When
    Date result = mappingUtils.parseDate(inputDate);

    // Then
    assertNotNull(result);
    // Verify the date is correctly parsed according to the AppContext settings
  }

  @Test
  void testConstantValue() {
    // Given
    String id = "testId";
    ConstantValue constantValue = new ConstantValue();
    constantValue.setDefaultValue("testValue");
    when(constantValueRepository.findById(id)).thenReturn(Optional.of(constantValue));

    // When
    String result = mappingUtils.constantValue(id);

    // Then
    assertEquals("testValue", result);
  }
}
