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
import com.etendorx.das.converter.ConversionException;
import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.converter.FieldConversionStrategy;
import com.etendorx.das.converter.strategy.ComputedMappingStrategy;
import com.etendorx.das.converter.strategy.ConstantValueStrategy;
import com.etendorx.das.converter.strategy.DirectMappingStrategy;
import com.etendorx.das.converter.strategy.EntityMappingStrategy;
import com.etendorx.das.converter.strategy.JavaMappingStrategy;
import com.etendorx.das.converter.strategy.JsonPathStrategy;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DynamicDTOConverter orchestrator.
 *
 * Tests cover:
 * - convertToMap: null entity, empty fields, strategy delegation, error handling, field order
 * - convertToEntity: null DTO, strategy delegation, mandatory validation, audit fields, fullDto context
 * - Strategy routing: each FieldMappingType routes to the correct strategy
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicDTOConverterTest {

    @Mock
    private DirectMappingStrategy directMappingStrategy;

    @Mock
    private ConstantValueStrategy constantValueStrategy;

    @Mock
    private ComputedMappingStrategy computedMappingStrategy;

    @Mock
    private EntityMappingStrategy entityMappingStrategy;

    @Mock
    private JavaMappingStrategy javaMappingStrategy;

    @Mock
    private JsonPathStrategy jsonPathStrategy;

    @Mock
    private DynamicMetadataService metadataService;

    @Mock
    private AuditServiceInterceptor auditServiceInterceptor;

    @Mock
    private EntityManager entityManager;

    private DynamicDTOConverter converter;

    @BeforeEach
    void setUp() {
        // Construct manually because the converter has many constructor params
        converter = new DynamicDTOConverter(
            metadataService,
            auditServiceInterceptor,
            entityManager,
            directMappingStrategy,
            constantValueStrategy,
            computedMappingStrategy,
            entityMappingStrategy,
            javaMappingStrategy,
            jsonPathStrategy
        );
    }

    // --- Helper methods ---

    /**
     * Creates a FieldMetadata record with given name, property, and field mapping type.
     * Uses sensible defaults for all other fields.
     */
    private FieldMetadata createFieldMetadata(String name, String property, FieldMappingType type) {
        return createFieldMetadata(name, property, type, false);
    }

    /**
     * Creates a FieldMetadata record with given name, property, field mapping type, and mandatory flag.
     */
    private FieldMetadata createFieldMetadata(String name, String property, FieldMappingType type,
                                               boolean mandatory) {
        return new FieldMetadata(
            "field-" + name,       // id
            name,                   // name
            property,               // property
            type,                   // fieldMapping
            mandatory,              // mandatory
            false,                  // identifiesUnivocally
            10L,                    // line
            null,                   // javaMappingQualifier
            null,                   // constantValue
            null,                   // jsonPath
            null,                   // relatedProjectionEntityId
            false                   // createRelated
        );
    }

    /**
     * Creates an EntityMetadata record with given id, name, and fields.
     */
    private EntityMetadata createEntityMetadata(String id, String name, List<FieldMetadata> fields) {
        return new EntityMetadata(
            id,
            name,
            "table-" + id,         // tableId
            "EW",                   // mappingType
            false,                  // identity
            true,                   // restEndPoint
            name,                   // externalName
            fields
        );
    }

    // ==========================================
    // convertToMap tests
    // ==========================================

    /**
     * Test: convertToMap returns null when entity is null.
     */
    @Test
    void convertToMap_nullEntity_returnsNull() {
        // Arrange
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());

        // Act
        Map<String, Object> result = converter.convertToMap(null, meta, meta.fields(), new ConversionContext());

        // Assert
        assertNull(result);
    }

    /**
     * Test: convertToMap returns empty map when fields list is empty.
     */
    @Test
    void convertToMap_emptyFields_returnsEmptyMap() {
        // Arrange
        Object entity = new Object();
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());

        // Act
        Map<String, Object> result = converter.convertToMap(entity, meta, meta.fields(), new ConversionContext());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test: convertToMap delegates to DirectMappingStrategy for DM field.
     * Verifies readField is called and result is placed in output map.
     */
    @Test
    void convertToMap_singleDMField_delegatesToDirectMapping() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));

        when(directMappingStrategy.readField(eq(entity), eq(field), any(ConversionContext.class)))
            .thenReturn("testValue");

        // Act
        Map<String, Object> result = converter.convertToMap(entity, meta, meta.fields(), new ConversionContext());

        // Assert
        assertNotNull(result);
        assertEquals("testValue", result.get("name"));
        verify(directMappingStrategy).readField(eq(entity), eq(field), any(ConversionContext.class));
    }

    /**
     * Test: convertToMap delegates to the correct strategy for each field type.
     * Creates DM, CV, and JM fields and verifies each strategy's readField is called exactly once.
     */
    @Test
    void convertToMap_multipleFieldTypes_delegatesToCorrectStrategies() {
        // Arrange
        Object entity = new Object();
        FieldMetadata dmField = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        FieldMetadata cvField = createFieldMetadata("status", null, FieldMappingType.CONSTANT_VALUE);
        FieldMetadata jmField = createFieldMetadata("computed", "sourceProperty", FieldMappingType.JAVA_MAPPING);

        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(dmField, cvField, jmField));

        when(directMappingStrategy.readField(eq(entity), eq(dmField), any(ConversionContext.class)))
            .thenReturn("nameValue");
        when(constantValueStrategy.readField(eq(entity), eq(cvField), any(ConversionContext.class)))
            .thenReturn("ACTIVE");
        when(javaMappingStrategy.readField(eq(entity), eq(jmField), any(ConversionContext.class)))
            .thenReturn("computedValue");

        // Act
        Map<String, Object> result = converter.convertToMap(entity, meta, meta.fields(), new ConversionContext());

        // Assert
        assertEquals(3, result.size());
        assertEquals("nameValue", result.get("name"));
        assertEquals("ACTIVE", result.get("status"));
        assertEquals("computedValue", result.get("computed"));

        verify(directMappingStrategy, times(1)).readField(eq(entity), eq(dmField), any(ConversionContext.class));
        verify(constantValueStrategy, times(1)).readField(eq(entity), eq(cvField), any(ConversionContext.class));
        verify(javaMappingStrategy, times(1)).readField(eq(entity), eq(jmField), any(ConversionContext.class));
    }

    /**
     * Test: convertToMap puts null for a field when its strategy throws an exception.
     * Verifies graceful degradation: exception is caught, null placed in result.
     */
    @Test
    void convertToMap_strategyThrows_putsNullForField() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));

        when(directMappingStrategy.readField(eq(entity), eq(field), any(ConversionContext.class)))
            .thenThrow(new RuntimeException("Simulated read error"));

        // Act
        Map<String, Object> result = converter.convertToMap(entity, meta, meta.fields(), new ConversionContext());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get("name"));
    }

    /**
     * Test: convertToMap preserves field order in the output LinkedHashMap.
     * Fields should appear in the same iteration order as they were in the input list.
     */
    @Test
    void convertToMap_preservesFieldOrder() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field1 = createFieldMetadata("alpha", "alphaProp", FieldMappingType.DIRECT_MAPPING);
        FieldMetadata field2 = createFieldMetadata("beta", "betaProp", FieldMappingType.DIRECT_MAPPING);
        FieldMetadata field3 = createFieldMetadata("gamma", "gammaProp", FieldMappingType.DIRECT_MAPPING);

        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field1, field2, field3));

        when(directMappingStrategy.readField(eq(entity), any(FieldMetadata.class), any(ConversionContext.class)))
            .thenReturn("value");

        // Act
        Map<String, Object> result = converter.convertToMap(entity, meta, meta.fields(), new ConversionContext());

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof LinkedHashMap, "Result should be a LinkedHashMap to preserve order");
        Iterator<String> keyIterator = result.keySet().iterator();
        assertEquals("alpha", keyIterator.next());
        assertEquals("beta", keyIterator.next());
        assertEquals("gamma", keyIterator.next());
    }

    // ==========================================
    // convertToEntity tests
    // ==========================================

    /**
     * Test: convertToEntity throws ConversionException when DTO is null.
     */
    @Test
    void convertToEntity_nullDto_throwsConversionException() {
        // Arrange
        Object entity = new Object();
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());

        // Act & Assert
        ConversionException exception = assertThrows(ConversionException.class, () -> {
            converter.convertToEntity(null, entity, meta, meta.fields());
        });
        assertTrue(exception.getMessage().contains("null"));
    }

    /**
     * Test: convertToEntity delegates to DirectMappingStrategy.writeField for DM field.
     */
    @Test
    void convertToEntity_singleDMField_delegatesToDirectMapping() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "testValue");

        // Act
        converter.convertToEntity(dto, entity, meta, meta.fields());

        // Assert
        verify(directMappingStrategy).writeField(eq(entity), eq("testValue"), eq(field), any(ConversionContext.class));
    }

    /**
     * Test: convertToEntity throws ConversionException when a mandatory DM field is missing from DTO.
     * The exception message should contain the field name.
     */
    @Test
    void convertToEntity_mandatoryFieldMissing_throwsConversionException() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING, true);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));
        Map<String, Object> dto = new HashMap<>(); // Missing "name" key

        // Act & Assert
        ConversionException exception = assertThrows(ConversionException.class, () -> {
            converter.convertToEntity(dto, entity, meta, meta.fields());
        });
        assertTrue(exception.getMessage().contains("name"),
            "Exception message should contain the mandatory field name");
    }

    /**
     * Test: convertToEntity does NOT throw when a mandatory DM field IS present in DTO.
     */
    @Test
    void convertToEntity_mandatoryFieldPresent_noException() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING, true);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "testValue");

        // Act & Assert (no exception expected)
        assertDoesNotThrow(() -> {
            converter.convertToEntity(dto, entity, meta, meta.fields());
        });
        verify(directMappingStrategy).writeField(eq(entity), eq("testValue"), eq(field), any(ConversionContext.class));
    }

    /**
     * Test: convertToEntity does NOT validate mandatory CV fields.
     * CV fields get their values from the database, not from DTO input.
     */
    @Test
    void convertToEntity_cvFieldMandatory_notValidated() {
        // Arrange
        Object entity = new Object();
        FieldMetadata cvField = new FieldMetadata(
            "field-status",         // id
            "status",               // name
            null,                   // property
            FieldMappingType.CONSTANT_VALUE, // fieldMapping
            true,                   // mandatory (but CV, so should not be validated)
            false,                  // identifiesUnivocally
            10L,                    // line
            null,                   // javaMappingQualifier
            "ACTIVE",               // constantValue
            null,                   // jsonPath
            null,                   // relatedProjectionEntityId
            false                   // createRelated
        );
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(cvField));
        Map<String, Object> dto = new HashMap<>(); // Missing "status" key intentionally

        // Act & Assert (no exception expected because CV is excluded from mandatory validation)
        assertDoesNotThrow(() -> {
            converter.convertToEntity(dto, entity, meta, meta.fields());
        });
    }

    /**
     * Test: convertToEntity calls auditServiceInterceptor.setAuditValues for BaseRXObject entities.
     */
    @Test
    void convertToEntity_auditFieldsSet_forBaseRXObject() {
        // Arrange
        BaseRXObject entity = mock(BaseRXObject.class);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();

        // Act
        converter.convertToEntity(dto, entity, meta, meta.fields());

        // Assert
        verify(auditServiceInterceptor).setAuditValues(entity);
    }

    /**
     * Test: convertToEntity does NOT call auditServiceInterceptor.setAuditValues for non-BaseRXObject entities.
     */
    @Test
    void convertToEntity_auditFieldsNotSet_forNonBaseRXObject() {
        // Arrange
        Object entity = new Object();
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();

        // Act
        converter.convertToEntity(dto, entity, meta, meta.fields());

        // Assert
        verify(auditServiceInterceptor, never()).setAuditValues(any(BaseRXObject.class));
    }

    /**
     * Test: convertToEntity sets the full DTO map on the ConversionContext.
     * Uses ArgumentCaptor to capture the ConversionContext passed to writeField and verify
     * that ctx.getFullDto() returns the input DTO.
     */
    @Test
    void convertToEntity_setsFullDtoOnContext() {
        // Arrange
        Object entity = new Object();
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata meta = createEntityMetadata("e1", "TestEntity", List.of(field));
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "testValue");

        ArgumentCaptor<ConversionContext> ctxCaptor = ArgumentCaptor.forClass(ConversionContext.class);

        // Act
        converter.convertToEntity(dto, entity, meta, meta.fields());

        // Assert
        verify(directMappingStrategy).writeField(eq(entity), eq("testValue"), eq(field), ctxCaptor.capture());
        ConversionContext capturedCtx = ctxCaptor.getValue();
        assertNotNull(capturedCtx.getFullDto());
        assertEquals(dto, capturedCtx.getFullDto());
    }
}
