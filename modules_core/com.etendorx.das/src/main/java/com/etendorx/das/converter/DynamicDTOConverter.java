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
package com.etendorx.das.converter;

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
import com.etendorx.das.metadata.models.ProjectionMetadata;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main orchestrator for bidirectional entity-to-map conversion.
 * Uses Phase 1 metadata to determine which strategy applies to each field,
 * orchestrates conversion across all 6 field mapping types, handles audit fields,
 * and validates mandatory fields on write.
 *
 * Replaces generated *DTOConverter classes with a single dynamic implementation.
 */
@Component
@Slf4j
public class DynamicDTOConverter {

    private final DynamicMetadataService metadataService;
    private final AuditServiceInterceptor auditServiceInterceptor;
    private final EntityManager entityManager;
    private final Map<FieldMappingType, FieldConversionStrategy> strategyMap;

    /**
     * Cache for table ID to Java class name lookups to avoid repeated DB queries.
     */
    private final ConcurrentHashMap<String, String> tableClassNameCache = new ConcurrentHashMap<>();

    public DynamicDTOConverter(
            DynamicMetadataService metadataService,
            AuditServiceInterceptor auditServiceInterceptor,
            EntityManager entityManager,
            DirectMappingStrategy directMappingStrategy,
            ConstantValueStrategy constantValueStrategy,
            ComputedMappingStrategy computedMappingStrategy,
            EntityMappingStrategy entityMappingStrategy,
            JavaMappingStrategy javaMappingStrategy,
            JsonPathStrategy jsonPathStrategy) {
        this.metadataService = metadataService;
        this.auditServiceInterceptor = auditServiceInterceptor;
        this.entityManager = entityManager;

        this.strategyMap = Map.of(
            FieldMappingType.DIRECT_MAPPING, directMappingStrategy,
            FieldMappingType.CONSTANT_VALUE, constantValueStrategy,
            FieldMappingType.CONSTANT_MAPPING, computedMappingStrategy,
            FieldMappingType.ENTITY_MAPPING, entityMappingStrategy,
            FieldMappingType.JAVA_MAPPING, javaMappingStrategy,
            FieldMappingType.JSON_PATH, jsonPathStrategy
        );
    }

    // --- READ PATH: Entity -> Map ---

    /**
     * Converts a JPA entity to a Map using projection metadata and field list.
     * Propagates ConversionContext for cycle detection in recursive EM conversions.
     *
     * @param entity         the JPA entity to convert
     * @param entityMetadata the entity metadata describing the projection entity
     * @param fields         the fields to convert (already sorted by line number)
     * @param ctx            the conversion context for cycle detection
     * @return a LinkedHashMap preserving field order, or null if entity is null
     */
    public Map<String, Object> convertToMap(Object entity, EntityMetadata entityMetadata,
                                             List<FieldMetadata> fields, ConversionContext ctx) {
        if (entity == null) {
            return null;
        }

        if (ctx == null) {
            ctx = new ConversionContext();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (FieldMetadata field : fields) {
            FieldConversionStrategy strategy = strategyMap.get(field.fieldMapping());
            if (strategy == null) {
                log.warn("No strategy for field mapping type: {} on field: {}",
                    field.fieldMapping(), field.name());
                continue;
            }

            try {
                Object value = strategy.readField(entity, field, ctx);
                result.put(field.name(), value);
            } catch (Exception e) {
                log.error("Error converting field '{}': {}", field.name(), e.getMessage());
                result.put(field.name(), null);
            }
        }

        return result;
    }

    /**
     * Convenience overload that creates a new ConversionContext.
     *
     * @param entity         the JPA entity to convert
     * @param entityMetadata the entity metadata describing the projection entity
     * @return a LinkedHashMap preserving field order, or null if entity is null
     */
    public Map<String, Object> convertToMap(Object entity, EntityMetadata entityMetadata) {
        return convertToMap(entity, entityMetadata, entityMetadata.fields(), new ConversionContext());
    }

    // --- WRITE PATH: Map -> Entity ---

    /**
     * Converts a DTO map to a JPA entity with mandatory validation and audit field integration.
     * If entity is null, instantiates a new entity using AD_Table javaClassName lookup.
     *
     * @param dto            the DTO map with field values
     * @param entity         the existing entity to populate, or null to create new
     * @param entityMetadata the entity metadata describing the projection entity
     * @param fields         the fields to write
     * @return the populated entity
     * @throws ConversionException if DTO is null, mandatory fields are missing, or entity instantiation fails
     */
    public Object convertToEntity(Map<String, Object> dto, Object entity,
                                   EntityMetadata entityMetadata, List<FieldMetadata> fields) {
        if (dto == null) {
            throw new ConversionException("DTO map cannot be null");
        }

        if (entity == null) {
            entity = instantiateEntity(entityMetadata);
        }

        ConversionContext ctx = new ConversionContext();
        ctx.setFullDto(dto);

        // Mandatory field validation (pre-check)
        validateMandatoryFields(dto, fields);

        // Field population: iterate fields sorted by line number
        for (FieldMetadata field : fields) {
            Object value = dto.get(field.name());

            FieldConversionStrategy strategy = strategyMap.get(field.fieldMapping());
            if (strategy == null) {
                log.warn("No strategy for field mapping type: {} on field: {}",
                    field.fieldMapping(), field.name());
                continue;
            }

            try {
                strategy.writeField(entity, value, field, ctx);
            } catch (ConversionException e) {
                // Re-throw field-specific conversion errors
                throw e;
            } catch (Exception e) {
                throw new ConversionException("Error setting field " + field.name(), e);
            }
        }

        // Audit fields: set client, org, active, createdBy, creationDate, updatedBy, updated
        if (entity instanceof BaseRXObject rxObj) {
            auditServiceInterceptor.setAuditValues(rxObj);
        }

        return entity;
    }

    /**
     * Convenience overload that creates a new entity from metadata.
     *
     * @param dto            the DTO map with field values
     * @param entityMetadata the entity metadata describing the projection entity
     * @return the populated entity
     */
    public Object convertToEntity(Map<String, Object> dto, EntityMetadata entityMetadata) {
        return convertToEntity(dto, null, entityMetadata, entityMetadata.fields());
    }

    // --- HELPER METHODS ---

    /**
     * Finds an EntityMetadata by its projection entity ID.
     * Used by EntityMappingStrategy to look up related entity metadata.
     * Iterates all projections to find the matching entity.
     *
     * @param projectionEntityId the unique ID of the projection entity
     * @return the matching EntityMetadata, or null if not found
     */
    public EntityMetadata findEntityMetadataById(String projectionEntityId) {
        if (projectionEntityId == null) {
            return null;
        }

        for (String projectionName : metadataService.getAllProjectionNames()) {
            ProjectionMetadata projection = metadataService.getProjection(projectionName)
                .orElse(null);
            if (projection == null) {
                continue;
            }

            for (EntityMetadata entityMeta : projection.entities()) {
                if (projectionEntityId.equals(entityMeta.id())) {
                    return entityMeta;
                }
            }
        }

        return null;
    }

    /**
     * Validates mandatory fields are present in the DTO.
     * Constants (CV, CM) are excluded since they don't come from DTO input.
     */
    private void validateMandatoryFields(Map<String, Object> dto, List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            if (field.mandatory()
                && field.fieldMapping() != FieldMappingType.CONSTANT_VALUE
                && field.fieldMapping() != FieldMappingType.CONSTANT_MAPPING
                && dto.get(field.name()) == null) {
                throw new ConversionException("Mandatory field missing: " + field.name());
            }
        }
    }

    /**
     * Instantiates a new entity using AD_Table javaClassName lookup.
     * Caches the class name to avoid repeated DB queries.
     */
    private Object instantiateEntity(EntityMetadata entityMetadata) {
        String tableId = entityMetadata.tableId();
        if (tableId == null) {
            throw new ConversionException(
                "Cannot determine entity class: tableId is null for entity " + entityMetadata.name());
        }

        String className = tableClassNameCache.computeIfAbsent(tableId, this::lookupJavaClassName);
        if (className == null) {
            throw new ConversionException(
                "Cannot determine entity class for table: " + tableId);
        }

        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConversionException(
                "Cannot instantiate entity class: " + className + " for table: " + tableId, e);
        }
    }

    /**
     * Queries AD_Table to get the Java class name for a given table ID.
     */
    private String lookupJavaClassName(String tableId) {
        try {
            return entityManager.createQuery(
                    "SELECT t.javaClassName FROM ADTable t WHERE t.id = :id", String.class)
                .setParameter("id", tableId)
                .getSingleResult();
        } catch (Exception e) {
            log.error("Could not look up javaClassName for tableId {}: {}", tableId, e.getMessage());
            return null;
        }
    }
}
