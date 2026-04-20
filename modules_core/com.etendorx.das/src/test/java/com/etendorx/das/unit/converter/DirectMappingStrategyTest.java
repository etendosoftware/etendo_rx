/*
 * Copyright 2022-2025  Futit Services SL
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
package com.etendorx.das.unit.converter;

import com.etendorx.das.converter.ConversionContext;
import com.etendorx.das.converter.PropertyAccessorService;
import com.etendorx.das.converter.strategy.DirectMappingStrategy;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.mappings.MappingUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DirectMappingStrategy (DM field type).
 *
 * Tests cover:
 * - Read path: getNestedProperty -> handleBaseObject pipeline
 * - Write path: setNestedProperty with type coercion for Date and numeric types
 * - Null handling on both read and write paths
 * - Nested property path delegation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DirectMappingStrategyTest {

    @Mock
    private PropertyAccessorService propertyAccessorService;

    @Mock
    private MappingUtils mappingUtils;

    @InjectMocks
    private DirectMappingStrategy strategy;

    private ConversionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ConversionContext();
    }

    // --- Helper methods ---

    /**
     * Creates a FieldMetadata record for DIRECT_MAPPING with given name and property.
     */
    private FieldMetadata createDMField(String name, String property) {
        return new FieldMetadata(
            "field-" + name,       // id
            name,                   // name
            property,               // property
            FieldMappingType.DIRECT_MAPPING, // fieldMapping
            false,                  // mandatory
            false,                  // identifiesUnivocally
            10L,                    // line
            null,                   // javaMappingQualifier
            null,                   // constantValue
            null,                   // jsonPath
            null,                   // relatedProjectionEntityId
            false                   // createRelated
        );
    }

    // --- Read path tests ---

    /**
     * Test: readField reads property via PropertyAccessorService and applies handleBaseObject.
     * Verifies the full pipeline: getNestedProperty -> handleBaseObject -> return.
     */
    @Test
    void readField_simpleProperty_returnsHandledValue() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("name", "nameProperty");

        when(propertyAccessorService.getNestedProperty(entity, "nameProperty")).thenReturn("rawValue");
        when(mappingUtils.handleBaseObject("rawValue")).thenReturn("processedValue");

        // Act
        Object result = strategy.readField(entity, field, ctx);

        // Assert
        assertEquals("processedValue", result);
        verify(propertyAccessorService).getNestedProperty(entity, "nameProperty");
        verify(mappingUtils).handleBaseObject("rawValue");
    }

    /**
     * Test: readField returns null when property value is null.
     * handleBaseObject should NOT be called for null values.
     */
    @Test
    void readField_nullProperty_returnsNull() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("name", "nameProperty");

        when(propertyAccessorService.getNestedProperty(entity, "nameProperty")).thenReturn(null);

        // Act
        Object result = strategy.readField(entity, field, ctx);

        // Assert
        assertNull(result);
        verify(propertyAccessorService).getNestedProperty(entity, "nameProperty");
        verify(mappingUtils, never()).handleBaseObject(any());
    }

    /**
     * Test: readField uses the correct nested property path from field metadata.
     * Verifies PropertyAccessorService is called with the dot-notation path "defaultrole.id".
     */
    @Test
    void readField_nestedProperty_readsCorrectPath() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("roleId", "defaultrole.id");

        when(propertyAccessorService.getNestedProperty(entity, "defaultrole.id")).thenReturn("role-123");
        when(mappingUtils.handleBaseObject("role-123")).thenReturn("role-123");

        // Act
        Object result = strategy.readField(entity, field, ctx);

        // Assert
        assertEquals("role-123", result);
        verify(propertyAccessorService).getNestedProperty(entity, "defaultrole.id");
    }

    /**
     * Test: readField formats Date values through handleBaseObject.
     * The MappingUtils.handleBaseObject() converts Date to formatted string.
     */
    @Test
    void readField_dateProperty_formatsViaHandleBaseObject() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("creationDate", "creationDate");
        Date testDate = new Date();

        when(propertyAccessorService.getNestedProperty(entity, "creationDate")).thenReturn(testDate);
        when(mappingUtils.handleBaseObject(testDate)).thenReturn("2026-02-06T12:00:00Z");

        // Act
        Object result = strategy.readField(entity, field, ctx);

        // Assert
        assertEquals("2026-02-06T12:00:00Z", result);
        verify(mappingUtils).handleBaseObject(testDate);
    }

    // --- Write path tests ---

    /**
     * Test: writeField sets a simple string value via PropertyAccessorService.
     * When PropertyUtils.getPropertyType does not throw, and value is String (not Date/Number),
     * falls through to direct set.
     */
    @Test
    void writeField_simpleValue_setsProperty() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("name", "nameProperty");
        String value = "testValue";

        // PropertyUtils.getPropertyType is a static call inside writeField; since we mock
        // propertyAccessorService at the service level, the strategy will try PropertyUtils
        // directly but catch the exception and fall through to set raw value.

        // Act
        strategy.writeField(entity, value, field, ctx);

        // Assert
        verify(propertyAccessorService).setNestedProperty(entity, "nameProperty", "testValue");
    }

    /**
     * Test: writeField sets null value via PropertyAccessorService.
     * Null values should be passed through directly without type coercion.
     */
    @Test
    void writeField_nullValue_setsNull() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createDMField("name", "nameProperty");

        // Act
        strategy.writeField(entity, null, field, ctx);

        // Assert
        verify(propertyAccessorService).setNestedProperty(entity, "nameProperty", null);
    }

    /**
     * Test: writeField performs Date coercion when target property type is Date and value is String.
     * Uses PropertyUtils.getPropertyType to detect the target type, then calls mappingUtils.parseDate.
     */
    @Test
    void writeField_dateCoercion_parsesString() {
        // Arrange
        FieldMetadata field = createDMField("creationDate", "creationDate");
        String dateString = "2026-02-06";
        Date parsedDate = new Date();

        // Create a test POJO with a Date property so PropertyUtils.getPropertyType returns Date.class
        TestEntityWithDate entity = new TestEntityWithDate();

        when(mappingUtils.parseDate(dateString)).thenReturn(parsedDate);

        // Act
        strategy.writeField(entity, dateString, field, ctx);

        // Assert
        verify(mappingUtils).parseDate(dateString);
        verify(propertyAccessorService).setNestedProperty(entity, "creationDate", parsedDate);
    }

    /**
     * Simple POJO for testing Date property type detection via PropertyUtils.getPropertyType.
     */
    public static class TestEntityWithDate {
        private Date creationDate;

        public Date getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(Date creationDate) {
            this.creationDate = creationDate;
        }
    }
}
