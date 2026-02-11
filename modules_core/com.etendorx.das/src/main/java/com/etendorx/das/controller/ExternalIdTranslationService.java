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
package com.etendorx.das.controller;

import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates external system IDs to internal IDs in incoming DTO maps.
 * Handles both the top-level "id" field and nested ENTITY_MAPPING reference fields.
 *
 * This service is consumed by DynamicRestController before passing DTOs to the repository layer,
 * ensuring the repository always receives internal IDs.
 */
@Component
@Slf4j
public class ExternalIdTranslationService {

    private final ExternalIdService externalIdService;
    private final DynamicDTOConverter converter;

    public ExternalIdTranslationService(ExternalIdService externalIdService,
                                         DynamicDTOConverter converter) {
        this.externalIdService = externalIdService;
        this.converter = converter;
    }

    /**
     * Translates external IDs in the given DTO map to internal IDs.
     * Mutates the dto map in place.
     *
     * <p>Two types of translations are performed:
     * <ol>
     *   <li>The top-level "id" field is translated using the entity's own tableId</li>
     *   <li>ENTITY_MAPPING reference fields are translated using the related entity's tableId</li>
     * </ol>
     *
     * @param dto         the mutable DTO map containing external IDs
     * @param entityMeta  the entity metadata describing the projection entity
     */
    public void translateExternalIds(Map<String, Object> dto, EntityMetadata entityMeta) {
        if (dto == null || entityMeta == null) {
            return;
        }

        // 1. Translate the "id" field if present
        translateTopLevelId(dto, entityMeta);

        // 2. Translate ENTITY_MAPPING reference fields
        translateEntityMappingFields(dto, entityMeta);
    }

    /**
     * Translates the top-level "id" field from external to internal ID.
     */
    private void translateTopLevelId(Map<String, Object> dto, EntityMetadata entityMeta) {
        Object idValue = dto.get("id");
        if (idValue == null) {
            return;
        }

        if (!(idValue instanceof String dtoId) || dtoId.isBlank()) {
            return;
        }

        String internalId = externalIdService.convertExternalToInternalId(
            entityMeta.tableId(), dtoId);
        dto.put("id", internalId);
        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Translated id '{}' -> '{}' (table: {})",
                dtoId, internalId, entityMeta.tableId());
        } else {
            log.debug("Translated top-level id '{}' -> '{}' for table {}", dtoId, internalId,
                entityMeta.tableId());
        }
    }

    /**
     * Iterates entity fields and translates ENTITY_MAPPING reference IDs.
     */
    private void translateEntityMappingFields(Map<String, Object> dto, EntityMetadata entityMeta) {
        for (FieldMetadata field : entityMeta.fields()) {
            if (shouldTranslateField(field, dto)) {
                translateField(field, dto, entityMeta);
            }
        }
    }

    private boolean shouldTranslateField(FieldMetadata field, Map<String, Object> dto) {
        return field.fieldMapping() == FieldMappingType.ENTITY_MAPPING 
            && dto.get(field.name()) != null;
    }

    private void translateField(FieldMetadata field, Map<String, Object> dto, EntityMetadata entityMeta) {
        Object value = dto.get(field.name());
        String referenceId = extractReferenceId(value, field.name());
        
        if (referenceId == null) {
            return;
        }

        EntityMetadata relatedEntityMeta = converter.findEntityMetadataById(
            field.relatedProjectionEntityId());
        
        if (relatedEntityMeta == null) {
            logMissingRelatedEntity(field);
            return;
        }

        String internalId = externalIdService.convertExternalToInternalId(
            relatedEntityMeta.tableId(), referenceId);

        replaceReferenceId(dto, field.name(), value, internalId);
        logTranslation(field, referenceId, internalId, relatedEntityMeta, entityMeta);
    }

    private void logMissingRelatedEntity(FieldMetadata field) {
        log.warn("Cannot translate external ID for field '{}': related entity metadata " +
            "not found for projectionEntityId '{}'", field.name(),
            field.relatedProjectionEntityId());
    }

    private void logTranslation(FieldMetadata field, String referenceId, String internalId,
                                EntityMetadata relatedEntityMeta, EntityMetadata entityMeta) {
        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Translated EM field '{}' id '{}' -> '{}' (table: {})",
                field.name(), referenceId, internalId, relatedEntityMeta.tableId());
        } else {
            log.debug("Translated EM field '{}' id '{}' -> '{}' using table {}",
                field.name(), referenceId, internalId, relatedEntityMeta.tableId());
        }
    }

    /**
     * Extracts the reference ID from a DTO field value.
     * The value can be either a String ID or a Map with an "id" key.
     *
     * @param value     the field value from the DTO
     * @param fieldName the field name (for logging)
     * @return the extracted reference ID, or null if extraction fails
     */
    private String extractReferenceId(Object value, String fieldName) {
        if (value instanceof String stringId) {
            return stringId.isBlank() ? null : stringId;
        }

        if (value instanceof Map<?, ?> mapValue) {
            Object idObj = mapValue.get("id");
            if (idObj instanceof String stringId) {
                return stringId.isBlank() ? null : stringId;
            }
            if (idObj != null) {
                log.warn("EM field '{}' map has non-String id value: {}", fieldName,
                    idObj.getClass().getSimpleName());
            }
            return null;
        }

        log.warn("EM field '{}' has unexpected value type: {}. Expected String or Map.",
            fieldName, value.getClass().getSimpleName());
        return null;
    }

    /**
     * Replaces the reference ID in the DTO, maintaining the original value structure.
     * If the original was a String, replaces with the translated String.
     * If the original was a Map, creates a new Map with the translated "id".
     */
    @SuppressWarnings("unchecked")
    private void replaceReferenceId(Map<String, Object> dto, String fieldName,
                                     Object originalValue, String internalId) {
        if (originalValue instanceof String) {
            dto.put(fieldName, internalId);
        } else if (originalValue instanceof Map<?, ?> originalMap) {
            Map<String, Object> updatedMap = new HashMap<>((Map<String, Object>) originalMap);
            updatedMap.put("id", internalId);
            dto.put(fieldName, updatedMap);
        }
    }
}
