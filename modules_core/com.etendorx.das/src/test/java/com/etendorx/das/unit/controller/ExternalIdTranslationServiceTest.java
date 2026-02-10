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
package com.etendorx.das.unit.controller;

import com.etendorx.das.controller.ExternalIdTranslationService;
import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExternalIdTranslationService.
 *
 * Tests cover:
 * - Top-level "id" field translation (external -> internal)
 * - Null/missing id field handling
 * - ENTITY_MAPPING String reference translation
 * - ENTITY_MAPPING Map reference translation (preserves Map structure)
 * - Skipping non-ENTITY_MAPPING fields (DIRECT_MAPPING)
 * - Skipping fields not present in DTO
 * - Passthrough when convertExternalToInternalId returns original value
 * - Multiple ENTITY_MAPPING fields translated independently
 */
@ExtendWith(MockitoExtension.class)
public class ExternalIdTranslationServiceTest {

    @Mock
    private ExternalIdService externalIdService;

    @Mock
    private DynamicDTOConverter converter;

    private ExternalIdTranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new ExternalIdTranslationService(externalIdService, converter);
    }

    // --- Helper methods ---

    private FieldMetadata createFieldMetadata(String name, FieldMappingType type,
                                               String relatedProjEntityId) {
        return new FieldMetadata(
            "field-" + name,           // id
            name,                       // name
            name + "Property",          // property
            type,                       // fieldMapping
            false,                      // mandatory
            false,                      // identifiesUnivocally
            10L,                        // line
            null,                       // javaMappingQualifier
            null,                       // constantValue
            null,                       // jsonPath
            relatedProjEntityId,        // relatedProjectionEntityId
            false                       // createRelated
        );
    }

    private EntityMetadata createEntityMetadata(String tableId, List<FieldMetadata> fields) {
        return new EntityMetadata(
            "entity-1",                 // id
            "TestEntity",               // name
            tableId,                    // tableId
            "EW",                       // mappingType
            false,                      // identity
            true,                       // restEndPoint
            "TestEntity",               // externalName
            fields,
            false                       // moduleInDevelopment
        );
    }

    // ==========================================
    // Top-level "id" translation tests
    // ==========================================

    /**
     * Test: translateExternalIds translates the top-level "id" field
     * from external to internal ID using the entity's own tableId.
     */
    @Test
    void translateExternalIds_translatesIdField() {
        // Arrange
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", "EXT-123");

        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of());

        when(externalIdService.convertExternalToInternalId("TABLE-1", "EXT-123"))
            .thenReturn("INT-456");

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        assertEquals("INT-456", dto.get("id"));
        verify(externalIdService).convertExternalToInternalId("TABLE-1", "EXT-123");
    }

    /**
     * Test: translateExternalIds does NOT call convertExternalToInternalId
     * when the DTO has no "id" key.
     */
    @Test
    void translateExternalIds_skipsNullIdField() {
        // Arrange
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "Test");
        // No "id" key in DTO

        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of());

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        verify(externalIdService, never()).convertExternalToInternalId(eq("TABLE-1"), anyString());
    }

    // ==========================================
    // ENTITY_MAPPING field translation tests
    // ==========================================

    /**
     * Test: translateExternalIds translates an ENTITY_MAPPING field
     * when the DTO value is a plain String (external ID).
     * The related entity's tableId is looked up via converter.findEntityMetadataById.
     */
    @Test
    void translateExternalIds_translatesEntityMappingStringReference() {
        // Arrange
        FieldMetadata orgField = createFieldMetadata("organization",
            FieldMappingType.ENTITY_MAPPING, "related-proj-entity-id");
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of(orgField));

        EntityMetadata relatedEntityMeta = new EntityMetadata(
            "related-proj-entity-id", "OrgEntity", "TABLE-ORG",
            "EW", false, true, "Organization", List.of(), false);

        when(converter.findEntityMetadataById("related-proj-entity-id"))
            .thenReturn(relatedEntityMeta);
        when(externalIdService.convertExternalToInternalId("TABLE-ORG", "EXT-ORG-1"))
            .thenReturn("INT-ORG-1");

        Map<String, Object> dto = new HashMap<>();
        dto.put("organization", "EXT-ORG-1");

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        assertEquals("INT-ORG-1", dto.get("organization"));
        verify(converter).findEntityMetadataById("related-proj-entity-id");
        verify(externalIdService).convertExternalToInternalId("TABLE-ORG", "EXT-ORG-1");
    }

    /**
     * Test: translateExternalIds translates an ENTITY_MAPPING field
     * when the DTO value is a Map with an "id" key. The Map structure
     * is preserved (only "id" updated), and other keys remain.
     */
    @Test
    void translateExternalIds_translatesEntityMappingMapReference() {
        // Arrange
        FieldMetadata orgField = createFieldMetadata("organization",
            FieldMappingType.ENTITY_MAPPING, "related-proj-entity-id");
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of(orgField));

        EntityMetadata relatedEntityMeta = new EntityMetadata(
            "related-proj-entity-id", "OrgEntity", "TABLE-ORG",
            "EW", false, true, "Organization", List.of(), false);

        when(converter.findEntityMetadataById("related-proj-entity-id"))
            .thenReturn(relatedEntityMeta);
        when(externalIdService.convertExternalToInternalId("TABLE-ORG", "EXT-ORG-1"))
            .thenReturn("INT-ORG-1");

        Map<String, Object> orgMap = new HashMap<>();
        orgMap.put("id", "EXT-ORG-1");
        orgMap.put("name", "Org Name");

        Map<String, Object> dto = new HashMap<>();
        dto.put("organization", orgMap);

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) dto.get("organization");
        assertEquals("INT-ORG-1", resultMap.get("id"));
        assertEquals("Org Name", resultMap.get("name"), "Non-id keys should be preserved");
    }

    // ==========================================
    // Skip / no-op tests
    // ==========================================

    /**
     * Test: translateExternalIds does NOT call convertExternalToInternalId
     * for DIRECT_MAPPING fields.
     */
    @Test
    void translateExternalIds_skipsDirectMappingFields() {
        // Arrange
        FieldMetadata nameField = createFieldMetadata("name",
            FieldMappingType.DIRECT_MAPPING, null);
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of(nameField));

        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "some-value");

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert - no external ID conversion for non-EM fields
        verify(externalIdService, never()).convertExternalToInternalId(anyString(), eq("some-value"));
        verify(converter, never()).findEntityMetadataById(anyString());
    }

    /**
     * Test: translateExternalIds skips ENTITY_MAPPING fields
     * whose name is not present in the DTO.
     */
    @Test
    void translateExternalIds_skipsFieldNotInDto() {
        // Arrange
        FieldMetadata orgField = createFieldMetadata("organization",
            FieldMappingType.ENTITY_MAPPING, "related-proj-entity-id");
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of(orgField));

        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "Test");
        // "organization" is NOT in the DTO

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        verify(converter, never()).findEntityMetadataById(anyString());
        verify(externalIdService, never()).convertExternalToInternalId(anyString(), anyString());
    }

    /**
     * Test: When convertExternalToInternalId returns the same value
     * (external ID not found in mappings), the DTO value remains the original.
     */
    @Test
    void translateExternalIds_handlesConvertReturningOriginalValue() {
        // Arrange
        FieldMetadata orgField = createFieldMetadata("organization",
            FieldMappingType.ENTITY_MAPPING, "related-proj-entity-id");
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1", List.of(orgField));

        EntityMetadata relatedEntityMeta = new EntityMetadata(
            "related-proj-entity-id", "OrgEntity", "TABLE-ORG",
            "EW", false, true, "Organization", List.of(), false);

        when(converter.findEntityMetadataById("related-proj-entity-id"))
            .thenReturn(relatedEntityMeta);
        // Returns the same value (external ID not found)
        when(externalIdService.convertExternalToInternalId("TABLE-ORG", "EXT-ORG-1"))
            .thenReturn("EXT-ORG-1");

        Map<String, Object> dto = new HashMap<>();
        dto.put("organization", "EXT-ORG-1");

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert - value stays the same
        assertEquals("EXT-ORG-1", dto.get("organization"));
    }

    /**
     * Test: Multiple ENTITY_MAPPING fields are each translated independently.
     * Both fields use their own related entity's tableId for conversion.
     */
    @Test
    void translateExternalIds_handlesMultipleEntityMappingFields() {
        // Arrange
        FieldMetadata orgField = createFieldMetadata("organization",
            FieldMappingType.ENTITY_MAPPING, "related-org-id");
        FieldMetadata warehouseField = createFieldMetadata("warehouse",
            FieldMappingType.ENTITY_MAPPING, "related-wh-id");
        EntityMetadata entityMeta = createEntityMetadata("TABLE-1",
            List.of(orgField, warehouseField));

        EntityMetadata orgMeta = new EntityMetadata(
            "related-org-id", "OrgEntity", "TABLE-ORG",
            "EW", false, true, "Organization", List.of(), false);
        EntityMetadata whMeta = new EntityMetadata(
            "related-wh-id", "WhEntity", "TABLE-WH",
            "EW", false, true, "Warehouse", List.of(), false);

        when(converter.findEntityMetadataById("related-org-id")).thenReturn(orgMeta);
        when(converter.findEntityMetadataById("related-wh-id")).thenReturn(whMeta);
        when(externalIdService.convertExternalToInternalId("TABLE-ORG", "EXT-ORG-1"))
            .thenReturn("INT-ORG-1");
        when(externalIdService.convertExternalToInternalId("TABLE-WH", "EXT-WH-1"))
            .thenReturn("INT-WH-1");

        Map<String, Object> dto = new HashMap<>();
        dto.put("organization", "EXT-ORG-1");
        dto.put("warehouse", "EXT-WH-1");

        // Act
        translationService.translateExternalIds(dto, entityMeta);

        // Assert
        assertEquals("INT-ORG-1", dto.get("organization"));
        assertEquals("INT-WH-1", dto.get("warehouse"));
        verify(converter).findEntityMetadataById("related-org-id");
        verify(converter).findEntityMetadataById("related-wh-id");
    }
}
