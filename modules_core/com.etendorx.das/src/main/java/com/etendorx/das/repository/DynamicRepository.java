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
package com.etendorx.das.repository;

import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.mapper.lib.DefaultValuesHandler;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.entities.mapper.lib.PostSyncService;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Generic dynamic repository providing CRUD and batch operations for any JPA entity
 * using runtime metadata from Phase 1 and conversion from Phase 2.
 *
 * Read operations use {@code @Transactional} for session management.
 * Write operations use manual {@link RestCallTransactionHandler} begin/commit
 * to match the generated BaseDTORepositoryDefault pattern (which disables/re-enables
 * PostgreSQL triggers around write operations).
 *
 * This class replaces the per-entity generated *DASRepository classes with a single
 * dynamic implementation that resolves entity classes via Hibernate metamodel.
 */
@Component
@Slf4j
public class DynamicRepository {

    private final EntityManager entityManager;
    private final DynamicDTOConverter converter;
    private final DynamicMetadataService metadataService;
    private final AuditServiceInterceptor auditService;
    private final RestCallTransactionHandler transactionHandler;
    private final ExternalIdService externalIdService;
    private final PostSyncService postSyncService;
    private final Validator validator;
    private final EntityClassResolver entityClassResolver;
    private final Optional<DefaultValuesHandler> defaultValuesHandler;

    public DynamicRepository(
            EntityManager entityManager,
            DynamicDTOConverter converter,
            DynamicMetadataService metadataService,
            AuditServiceInterceptor auditService,
            RestCallTransactionHandler transactionHandler,
            ExternalIdService externalIdService,
            PostSyncService postSyncService,
            Validator validator,
            EntityClassResolver entityClassResolver,
            Optional<DefaultValuesHandler> defaultValuesHandler) {
        this.entityManager = entityManager;
        this.converter = converter;
        this.metadataService = metadataService;
        this.auditService = auditService;
        this.transactionHandler = transactionHandler;
        this.externalIdService = externalIdService;
        this.postSyncService = postSyncService;
        this.validator = validator;
        this.entityClassResolver = entityClassResolver;
        this.defaultValuesHandler = defaultValuesHandler;
    }

    // --- READ OPERATIONS (use @Transactional for JPA session management) ---

    /**
     * Finds a single entity by ID, converts it to a Map using projection metadata.
     *
     * @param id             the entity primary key (internal ID)
     * @param projectionName the projection name
     * @param entityName     the entity name within the projection
     * @return the entity as a Map of field name to value
     * @throws DynamicRepositoryException if metadata or entity class cannot be resolved
     * @throws EntityNotFoundException    if the entity does not exist
     */
    @Transactional
    public Map<String, Object> findById(String id, String projectionName, String entityName) {
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new DynamicRepositoryException(
                "Entity metadata not found for projection: " + projectionName + ", entity: " + entityName));

        Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.findById | entity={} class={} id={}",
                entityName, entityClass.getSimpleName(), id);
        }

        Object entity = entityManager.find(entityClass, id);
        if (entity == null) {
            if (entityMeta.moduleInDevelopment()) {
                log.info("[X-Ray] Repository.findById | not found id={}", id);
            }
            throw new EntityNotFoundException(
                "Entity " + entityName + " not found with id: " + id);
        }

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.findById | found id={}", id);
        }

        return converter.convertToMap(entity, entityMeta);
    }

    /**
     * Finds all entities matching the given filters with pagination and sorting.
     * Only DIRECT_MAPPING fields are supported for filtering (other mapping types
     * don't have direct entity properties to query against).
     *
     * @param projectionName the projection name
     * @param entityName     the entity name within the projection
     * @param filters        field name to value filters (equality match)
     * @param pageable       pagination and sorting parameters
     * @return a Page of entities converted to Maps
     * @throws DynamicRepositoryException if metadata or entity class cannot be resolved
     */
    @Transactional
    public Page<Map<String, Object>> findAll(String projectionName, String entityName,
                                              Map<String, String> filters, Pageable pageable) {
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new DynamicRepositoryException(
                "Entity metadata not found for projection: " + projectionName + ", entity: " + entityName));

        Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.findAll | entity={} class={} filters={}",
                entityName, entityClass.getSimpleName(),
                filters != null ? filters.size() : 0);
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query for total elements
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<?> countRoot = countQuery.from(entityClass);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, filters, entityMeta.fields());
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(new Predicate[0]));
        }
        long total = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<Object> dataQuery = (CriteriaQuery<Object>) cb.createQuery(entityClass);
        Root<?> dataRoot = dataQuery.from(entityClass);
        dataQuery.select((Selection<? extends Object>) dataRoot);
        List<Predicate> dataPredicates = buildPredicates(cb, dataRoot, filters, entityMeta.fields());
        if (!dataPredicates.isEmpty()) {
            dataQuery.where(dataPredicates.toArray(new Predicate[0]));
        }

        // Sorting
        if (pageable.getSort().isSorted()) {
            List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
            for (Sort.Order sortOrder : pageable.getSort()) {
                Path<?> sortPath = buildPath(dataRoot, sortOrder.getProperty());
                if (sortOrder.isAscending()) {
                    orders.add(cb.asc(sortPath));
                } else {
                    orders.add(cb.desc(sortPath));
                }
            }
            dataQuery.orderBy(orders);
        }

        // Pagination
        TypedQuery<?> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Execute and convert
        List<?> results = typedQuery.getResultList();
        List<Map<String, Object>> converted = results.stream()
            .map(entity -> converter.convertToMap(entity, entityMeta))
            .toList();

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.findAll | entity={} class={} total={} page={}/{}",
                entityName, entityClass.getSimpleName(), total,
                pageable.getPageNumber(),
                total > 0 ? (total - 1) / pageable.getPageSize() : 0);
        }

        return new PageImpl<>(converted, pageable, total);
    }

    // --- HELPER METHODS (filtering) ---

    /**
     * Builds JPA Criteria predicates from filter parameters.
     * Only supports DIRECT_MAPPING fields since other mapping types (EM, JM, CV, JP)
     * don't have direct entity properties to filter on.
     *
     * @param cb      the CriteriaBuilder
     * @param root    the query root
     * @param filters the filter name-value pairs (DTO field names)
     * @param fields  the field metadata list to resolve DTO names to entity properties
     * @return list of equality predicates
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<?> root,
                                             Map<String, String> filters,
                                             List<FieldMetadata> fields) {
        List<Predicate> predicates = new ArrayList<>();
        if (filters == null || filters.isEmpty()) {
            return predicates;
        }

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String dtoFieldName = filter.getKey();
            String value = filter.getValue();

            // Find matching DIRECT_MAPPING field to get entity property path
            FieldMetadata field = fields.stream()
                .filter(f -> f.name().equals(dtoFieldName)
                    && f.fieldMapping() == FieldMappingType.DIRECT_MAPPING)
                .findFirst()
                .orElse(null);

            if (field != null && field.property() != null) {
                Path<?> path = buildPath(root, field.property());
                predicates.add(cb.equal(path, value));
            }
        }

        return predicates;
    }

    /**
     * Builds a JPA Path from a potentially nested property expression (e.g., "organization.id").
     *
     * @param root         the query root
     * @param propertyPath the dot-separated property path
     * @return the resolved Path
     */
    private Path<?> buildPath(Root<?> root, String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    // --- WRITE OPERATIONS (use manual transactionHandler, NOT @Transactional) ---

    /**
     * Saves a new entity from a DTO map.
     * Delegates to {@link #performSaveOrUpdate} with isNew=true.
     *
     * @param dto            the DTO map with field values
     * @param projectionName the projection name
     * @param entityName     the entity name within the projection
     * @return the saved entity as a Map
     */
    public Map<String, Object> save(Map<String, Object> dto, String projectionName, String entityName) {
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new DynamicRepositoryException(
                "Entity metadata not found for projection: " + projectionName + ", entity: " + entityName));
        return performSaveOrUpdate(dto, entityMeta, true);
    }

    /**
     * Updates an existing entity from a DTO map.
     * Delegates to {@link #performSaveOrUpdate} with isNew=false.
     *
     * @param dto            the DTO map with field values
     * @param projectionName the projection name
     * @param entityName     the entity name within the projection
     * @return the updated entity as a Map
     */
    public Map<String, Object> update(Map<String, Object> dto, String projectionName, String entityName) {
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new DynamicRepositoryException(
                "Entity metadata not found for projection: " + projectionName + ", entity: " + entityName));
        return performSaveOrUpdate(dto, entityMeta, false);
    }

    /**
     * Saves a batch of entities in a single transaction.
     * All entities share the same transactionHandler.begin/commit lifecycle.
     * If any entity fails, the entire batch rolls back.
     *
     * @param dtos           list of DTO maps to save
     * @param projectionName the projection name
     * @param entityName     the entity name within the projection
     * @return list of saved entities as Maps
     */
    public List<Map<String, Object>> saveBatch(List<Map<String, Object>> dtos,
                                                String projectionName, String entityName) {
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new DynamicRepositoryException(
                "Entity metadata not found for projection: " + projectionName + ", entity: " + entityName));

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            if (entityMeta.moduleInDevelopment()) {
                log.info("[X-Ray] Repository.saveBatch | size={} entity={}",
                    dtos.size(), entityName);
            }
            transactionHandler.begin();
            for (Map<String, Object> dto : dtos) {
                Map<String, Object> result = performSaveOrUpdateInternal(dto, entityMeta, true);
                results.add(result);
            }
            transactionHandler.commit();
            if (entityMeta.moduleInDevelopment()) {
                log.info("[X-Ray] Repository.saveBatch | committed {} items", results.size());
            }
            return results;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    // --- WRITE HELPERS ---

    /**
     * Wraps {@link #performSaveOrUpdateInternal} with its own transactionHandler begin/commit.
     * Used by single save/update operations. Batch operations call performSaveOrUpdateInternal
     * directly within their own transaction scope.
     *
     * @param dto        the DTO map
     * @param entityMeta the entity metadata
     * @param isNew      true for create, false for update (may be overridden by upsert logic)
     * @return the saved/updated entity as a Map
     */
    private Map<String, Object> performSaveOrUpdate(Map<String, Object> dto,
                                                     EntityMetadata entityMeta, boolean isNew) {
        try {
            transactionHandler.begin();
            Map<String, Object> result = performSaveOrUpdateInternal(dto, entityMeta, isNew);
            transactionHandler.commit();
            return result;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    /**
     * Core save/update implementation replicating the exact order of operations
     * from BaseDTORepositoryDefault.performSaveOrUpdate(), with two critical differences:
     *
     * 1. New entities are pre-instantiated via {@link EntityClassResolver} + newInstance(),
     *    ensuring the converter NEVER triggers its internal AD_Table.javaClassName lookup.
     * 2. Audit values are NOT set here -- {@link DynamicDTOConverter#convertToEntity} already
     *    calls auditServiceInterceptor.setAuditValues() internally (lines 192-194).
     *
     * @param dto        the DTO map
     * @param entityMeta the entity metadata
     * @param isNewParam initial new/update hint (may be overridden by upsert check)
     * @return the saved entity as a Map
     */
    private Map<String, Object> performSaveOrUpdateInternal(Map<String, Object> dto,
                                                             EntityMetadata entityMeta,
                                                             boolean isNewParam) {
        boolean isNew = isNewParam;
        Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());
        Object existingEntity = null;
        String dtoId = (String) dto.get("id");

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.save | operation={} class={} dtoId={}",
                isNew ? "INSERT" : "UPDATE", entityClass.getSimpleName(), dtoId);
        }

        // Upsert: check existence when ID provided
        if (dtoId != null) {
            existingEntity = entityManager.find(entityClass, dtoId);
            if (existingEntity != null) {
                isNew = false;
            }
        }

        // CRITICAL: Pre-instantiate new entity via metamodel if no existing entity found.
        // This ensures convertToEntity() never hits its internal AD_Table.javaClassName path.
        if (existingEntity == null) {
            try {
                existingEntity = entityClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new DynamicRepositoryException(
                    "Cannot instantiate entity class: " + entityClass.getName(), e);
            }
        }

        // Convert DTO to entity -- converter receives non-null entity, skips instantiation.
        // NOTE: converter also calls auditService.setAuditValues() internally. Do NOT call it again here.
        Object entity = converter.convertToEntity(dto, existingEntity, entityMeta, entityMeta.fields());

        // Default values (if handler exists)
        defaultValuesHandler.ifPresent(h -> h.setDefaultValues(entity));

        // Validate (skip "id" violations) -- audit was already set by converter
        validateEntity(entity);

        // First save
        Object mergedEntity = entityManager.merge(entity);
        entityManager.flush();

        if (entityMeta.moduleInDevelopment()) {
            log.info("[X-Ray] Repository.save | merged, newId={}", getEntityId(mergedEntity));
        }

        // External ID registration (AFTER merge so entity has ID)
        String tableId = entityMeta.tableId();
        externalIdService.add(tableId, dtoId, mergedEntity);
        externalIdService.flush();

        // Second save (after potential list processing)
        mergedEntity = entityManager.merge(mergedEntity);
        postSyncService.flush();
        externalIdService.flush();

        // Return freshly read result
        String newId = getEntityId(mergedEntity);
        Object freshEntity = entityManager.find(entityClass, newId);
        return converter.convertToMap(freshEntity, entityMeta);
    }

    /**
     * Validates entity using Jakarta Validator, skipping "id" property violations.
     * Generated entities have {@code @NotNull} on the ID field, but JPA generates
     * the ID during persist, so "id: must not be null" is expected for new entities.
     *
     * @param entity the entity to validate
     * @throws ResponseStatusException with BAD_REQUEST if non-id violations exist
     */
    private void validateEntity(Object entity) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            List<String> messages = new ArrayList<>();
            boolean hasViolations = false;
            for (ConstraintViolation<Object> violation : violations) {
                // Skip "id" path -- JPA generates ID, so it's null before persist
                if (!StringUtils.equals(violation.getPropertyPath().toString(), "id")) {
                    messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
                    hasViolations = true;
                }
            }
            if (hasViolations) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Validation failed: " + messages);
            }
        }
    }

    /**
     * Extracts the entity ID using BeanUtils PropertyUtils.
     * All generated entities have a String "id" property.
     *
     * @param entity the JPA entity
     * @return the entity ID as a String
     * @throws DynamicRepositoryException if ID extraction fails
     */
    private String getEntityId(Object entity) {
        try {
            Object id = PropertyUtils.getProperty(entity, "id");
            return (String) id;
        } catch (Exception e) {
            throw new DynamicRepositoryException(
                "Cannot extract ID from entity: " + entity.getClass().getName(), e);
        }
    }
}
