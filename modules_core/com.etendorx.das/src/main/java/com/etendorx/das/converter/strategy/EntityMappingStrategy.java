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
package com.etendorx.das.converter.strategy;

import com.etendorx.das.converter.ConversionContext;
import com.etendorx.das.converter.ConversionException;
import com.etendorx.das.converter.FieldConversionStrategy;
import com.etendorx.das.converter.PropertyAccessorService;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for ENTITY_MAPPING (EM) field types.
 * Handles related entity conversion: recursively converts related entities into nested Maps on read,
 * and resolves entity references by externalId on write using ExternalIdService.
 * Detects cycles and returns id+identifier stub for already-visited entities.
 */
@Component
@Slf4j
public class EntityMappingStrategy implements FieldConversionStrategy {

    private final PropertyAccessorService propertyAccessorService;
    private final DynamicMetadataService metadataService;
    private final ExternalIdService externalIdService;
    private final EntityManager entityManager;
    private final Object dynamicDTOConverterRef;

    /**
     * Constructor with @Lazy on DynamicDTOConverter to break circular dependency.
     * EntityMappingStrategy -> DynamicDTOConverter -> EntityMappingStrategy would cause a cycle.
     */
    public EntityMappingStrategy(
            PropertyAccessorService propertyAccessorService,
            DynamicMetadataService metadataService,
            ExternalIdService externalIdService,
            EntityManager entityManager,
            @Lazy com.etendorx.das.converter.DynamicDTOConverter dynamicDTOConverter) {
        this.propertyAccessorService = propertyAccessorService;
        this.metadataService = metadataService;
        this.externalIdService = externalIdService;
        this.entityManager = entityManager;
        this.dynamicDTOConverterRef = dynamicDTOConverter;
    }

    /**
     * Gets the lazily injected DynamicDTOConverter.
     */
    private com.etendorx.das.converter.DynamicDTOConverter getDynamicDTOConverter() {
        return (com.etendorx.das.converter.DynamicDTOConverter) dynamicDTOConverterRef;
    }

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        Object relatedEntity = propertyAccessorService.getNestedProperty(entity, field.property());

        if (relatedEntity == null) {
            return null;
        }

        // Handle one-to-many (Collection)
        if (relatedEntity instanceof Collection<?> collection) {
            return readCollection(collection, field, ctx);
        }

        // Handle many-to-one (single entity)
        return readSingleEntity(relatedEntity, field, ctx);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        if (value == null) {
            propertyAccessorService.setNestedProperty(entity, field.property(), null);
            return;
        }

        String referenceId = extractReferenceId(value);
        if (referenceId == null) {
            log.warn("Could not extract reference ID from value for field: {}", field.name());
            return;
        }

        // Resolve the related entity metadata to get the tableId
        EntityMetadata relatedMeta = findRelatedEntityMetadata(field.relatedProjectionEntityId());
        if (relatedMeta == null) {
            log.warn("Could not find related entity metadata for field: {}, relatedProjectionEntityId: {}",
                field.name(), field.relatedProjectionEntityId());
            return;
        }

        // Convert external ID to internal ID
        String internalId = externalIdService.convertExternalToInternalId(
            relatedMeta.tableId(), referenceId);

        // Load the related entity via JPQL using the entity name from metadata
        Object relatedEntity = loadRelatedEntity(relatedMeta, internalId);
        if (relatedEntity == null) {
            log.warn("Could not load related entity for field: {}, id: {}", field.name(), internalId);
            return;
        }

        propertyAccessorService.setNestedProperty(entity, field.property(), relatedEntity);
    }

    /**
     * Reads a collection of related entities, converting each to a Map.
     */
    private List<Map<String, Object>> readCollection(Collection<?> collection, FieldMetadata field,
                                                      ConversionContext ctx) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object element : collection) {
            if (element == null) {
                result.add(null);
                continue;
            }

            // Cycle detection per element
            if (ctx.isVisited(element)) {
                result.add(createStub(element));
                continue;
            }

            EntityMetadata relatedMeta = findRelatedEntityMetadata(field.relatedProjectionEntityId());
            if (relatedMeta == null) {
                result.add(createStub(element));
            } else {
                Map<String, Object> converted = getDynamicDTOConverter().convertToMap(
                    element, relatedMeta, relatedMeta.fields(), ctx);
                result.add(converted);
            }
        }

        return result;
    }

    /**
     * Reads a single related entity (many-to-one), converting it to a Map.
     */
    private Map<String, Object> readSingleEntity(Object relatedEntity, FieldMetadata field,
                                                  ConversionContext ctx) {
        // Cycle detection
        if (ctx.isVisited(relatedEntity)) {
            return createStub(relatedEntity);
        }

        // Look up related entity's metadata
        EntityMetadata relatedMeta = findRelatedEntityMetadata(field.relatedProjectionEntityId());
        if (relatedMeta == null) {
            // Fall back to id+identifier stub if metadata not found
            log.debug("No metadata found for relatedProjectionEntityId: {}, returning stub",
                field.relatedProjectionEntityId());
            return createStub(relatedEntity);
        }

        // Recursively convert the related entity
        return getDynamicDTOConverter().convertToMap(
            relatedEntity, relatedMeta, relatedMeta.fields(), ctx);
    }

    /**
     * Creates a stub Map with id and _identifier for cycle detection or fallback.
     */
    private Map<String, Object> createStub(Object entity) {
        Map<String, Object> stub = new HashMap<>();
        if (entity instanceof BaseRXObject rxObj) {
            stub.put("id", propertyAccessorService.getNestedProperty(entity, "id"));
            stub.put("_identifier", rxObj.get_identifier());
        }
        return stub;
    }

    /**
     * Extracts the reference ID from a value which can be a Map (with "id" key) or a String.
     */
    @SuppressWarnings("unchecked")
    private String extractReferenceId(Object value) {
        if (value instanceof Map) {
            Object id = ((Map<String, Object>) value).get("id");
            return id != null ? id.toString() : null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        log.warn("Unexpected value type for EM field write: {}", value.getClass().getName());
        return null;
    }

    /**
     * Finds the EntityMetadata for a related projection entity by its ID.
     * Iterates all projections to find the matching entity.
     */
    private EntityMetadata findRelatedEntityMetadata(String projectionEntityId) {
        if (projectionEntityId == null) {
            return null;
        }
        return getDynamicDTOConverter().findEntityMetadataById(projectionEntityId);
    }

    /**
     * Loads a related entity using JPQL query by entity name and internal ID.
     */
    private Object loadRelatedEntity(EntityMetadata relatedMeta, String internalId) {
        if (internalId == null) {
            return null;
        }
        try {
            return entityManager.createQuery(
                    "SELECT e FROM " + relatedMeta.name() + " e WHERE e.id = :id")
                .setParameter("id", internalId)
                .getSingleResult();
        } catch (Exception e) {
            log.warn("Could not load entity {} with id {}: {}",
                relatedMeta.name(), internalId, e.getMessage());
            return null;
        }
    }
}
