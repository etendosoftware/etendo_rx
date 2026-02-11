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
import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.converter.PropertyAccessorService;
import com.etendorx.das.converter.strategy.EntityMappingStrategy;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityMappingStrategy (EM field type).
 *
 * Tests cover:
 * - Read path: null related entity, cycle detection with stub, recursive conversion
 * - Write path: Map with "id" key resolved via ExternalId, null value, String ID resolution
 * - Cycle detection returns id + _identifier stub for already-visited entities
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EntityMappingStrategyTest {

    @Mock
    private PropertyAccessorService propertyAccessorService;

    @Mock
    private DynamicMetadataService metadataService;

    @Mock
    private ExternalIdService externalIdService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private DynamicDTOConverter dynamicDTOConverter;

    private EntityMappingStrategy strategy;

    private ConversionContext ctx;

    @BeforeEach
    void setUp() {
        // Construct manually because @Lazy parameter prevents @InjectMocks usage
        strategy = new EntityMappingStrategy(
            propertyAccessorService,
            metadataService,
            externalIdService,
            entityManager,
            dynamicDTOConverter
        );
        ctx = new ConversionContext();
    }

    // --- Helper methods ---

    /**
     * Creates a FieldMetadata record for ENTITY_MAPPING with given name, property,
     * and relatedProjectionEntityId.
     */
    private FieldMetadata createEMField(String name, String property, String relatedProjectionEntityId) {
        return new FieldMetadata(
            "field-" + name,       // id
            name,                   // name
            property,               // property
            FieldMappingType.ENTITY_MAPPING, // fieldMapping
            false,                  // mandatory
            false,                  // identifiesUnivocally
            10L,                    // line
            null,                   // javaMappingQualifier
            null,                   // constantValue
            null,                   // jsonPath
            relatedProjectionEntityId, // relatedProjectionEntityId
            false                   // createRelated
        );
    }

    /**
     * Creates an EntityMetadata record with sensible defaults.
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
            fields,
            false                   // moduleInDevelopment
        );
    }

    // --- Read path tests ---

    /**
     * Test: readField returns null when related entity property is null.
     * No recursive conversion should be attempted.
     */
    @Test
    void readField_relatedEntityNull_returnsNull() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");

        when(propertyAccessorService.getNestedProperty(parentEntity, "organization")).thenReturn(null);

        // Act
        Object result = strategy.readField(parentEntity, field, ctx);

        // Assert
        assertNull(result);
        verify(dynamicDTOConverter, never()).convertToMap(any(), any(), anyList(), any());
    }

    /**
     * Test: readField returns stub Map with id and _identifier for already-visited entities.
     * This verifies cycle detection: if an entity was already converted in the current context,
     * the strategy returns a minimal stub instead of recursing infinitely.
     */
    @Test
    @SuppressWarnings("unchecked")
    void readField_cycleDetected_returnsStub() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");

        // Create a mock BaseRXObject as the related entity
        BaseRXObject relatedEntity = mock(BaseRXObject.class);
        when(relatedEntity.get_identifier()).thenReturn("Test Organization");

        when(propertyAccessorService.getNestedProperty(parentEntity, "organization"))
            .thenReturn(relatedEntity);
        when(propertyAccessorService.getNestedProperty(relatedEntity, "id"))
            .thenReturn("org-123");

        // Pre-visit the entity to simulate it already being converted (cycle)
        ctx.isVisited(relatedEntity);

        // Act
        Object result = strategy.readField(parentEntity, field, ctx);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<String, Object> stub = (Map<String, Object>) result;
        assertEquals("org-123", stub.get("id"));
        assertEquals("Test Organization", stub.get("_identifier"));
        assertEquals(2, stub.size());

        // Verify no recursive conversion was attempted
        verify(dynamicDTOConverter, never()).convertToMap(any(), any(), anyList(), any());
    }

    /**
     * Test: readField recursively converts a related entity via DynamicDTOConverter.
     * The strategy looks up related entity metadata and delegates to converter.convertToMap.
     */
    @Test
    @SuppressWarnings("unchecked")
    void readField_normalEntity_recursivelyConverts() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");

        BaseRXObject relatedEntity = mock(BaseRXObject.class);
        when(relatedEntity.get_identifier()).thenReturn("Test Organization");

        when(propertyAccessorService.getNestedProperty(parentEntity, "organization"))
            .thenReturn(relatedEntity);
        when(propertyAccessorService.getNestedProperty(relatedEntity, "id"))
            .thenReturn("org-123");

        // Mock metadata lookup via DynamicDTOConverter.findEntityMetadataById
        EntityMetadata relatedMeta = createEntityMetadata("related-entity-1", "Organization",
            Collections.emptyList());
        when(dynamicDTOConverter.findEntityMetadataById("related-entity-1"))
            .thenReturn(relatedMeta);

        // Mock the recursive conversion result
        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("id", "org-123");
        expectedResult.put("name", "Test Organization");
        when(dynamicDTOConverter.convertToMap(eq(relatedEntity), eq(relatedMeta), eq(relatedMeta.fields()), any(ConversionContext.class)))
            .thenReturn(expectedResult);

        // Act
        Object result = strategy.readField(parentEntity, field, ctx);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("org-123", resultMap.get("id"));
        assertEquals("Test Organization", resultMap.get("name"));

        verify(dynamicDTOConverter).convertToMap(eq(relatedEntity), eq(relatedMeta), eq(relatedMeta.fields()), any(ConversionContext.class));
    }

    // --- Write path tests ---

    /**
     * Test: writeField resolves entity by external ID from a Map with "id" key.
     * Uses ExternalIdService to convert external -> internal ID, then loads entity via JPQL.
     */
    @Test
    @SuppressWarnings("unchecked")
    void writeField_mapWithId_resolvesViaExternalId() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");

        Map<String, Object> valueMap = Map.of("id", "ext-org-456");

        // Mock metadata lookup
        EntityMetadata relatedMeta = createEntityMetadata("related-entity-1", "Organization",
            Collections.emptyList());
        when(dynamicDTOConverter.findEntityMetadataById("related-entity-1"))
            .thenReturn(relatedMeta);

        // Mock ExternalId resolution
        when(externalIdService.convertExternalToInternalId("table-related-entity-1", "ext-org-456"))
            .thenReturn("internal-org-789");

        // Mock JPQL entity loading
        Object resolvedEntity = new Object();
        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("id"), eq("internal-org-789"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(resolvedEntity);

        // Act
        strategy.writeField(parentEntity, valueMap, field, ctx);

        // Assert
        verify(externalIdService).convertExternalToInternalId("table-related-entity-1", "ext-org-456");
        verify(propertyAccessorService).setNestedProperty(parentEntity, "organization", resolvedEntity);
    }

    /**
     * Test: writeField sets null on entity property when value is null.
     */
    @Test
    void writeField_nullValue_setsNull() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");

        // Act
        strategy.writeField(parentEntity, null, field, ctx);

        // Assert
        verify(propertyAccessorService).setNestedProperty(parentEntity, "organization", null);
        verify(externalIdService, never()).convertExternalToInternalId(any(), any());
    }

    /**
     * Test: writeField resolves entity when value is a String (direct ID).
     * ExternalIdService is called with the String value directly.
     */
    @Test
    @SuppressWarnings("unchecked")
    void writeField_stringId_resolvesDirectly() {
        // Arrange
        Object parentEntity = new Object();
        FieldMetadata field = createEMField("organization", "organization", "related-entity-1");
        String stringId = "ext-org-direct-456";

        // Mock metadata lookup
        EntityMetadata relatedMeta = createEntityMetadata("related-entity-1", "Organization",
            Collections.emptyList());
        when(dynamicDTOConverter.findEntityMetadataById("related-entity-1"))
            .thenReturn(relatedMeta);

        // Mock ExternalId resolution
        when(externalIdService.convertExternalToInternalId("table-related-entity-1", "ext-org-direct-456"))
            .thenReturn("internal-org-direct-789");

        // Mock JPQL entity loading
        Object resolvedEntity = new Object();
        TypedQuery mockQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(eq("id"), eq("internal-org-direct-789"))).thenReturn(mockQuery);
        when(mockQuery.getSingleResult()).thenReturn(resolvedEntity);

        // Act
        strategy.writeField(parentEntity, stringId, field, ctx);

        // Assert
        verify(externalIdService).convertExternalToInternalId("table-related-entity-1", "ext-org-direct-456");
        verify(propertyAccessorService).setNestedProperty(parentEntity, "organization", resolvedEntity);
    }
}
