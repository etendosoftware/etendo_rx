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
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

        Object entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new EntityNotFoundException(
                "Entity " + entityName + " not found with id: " + id);
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
        CriteriaQuery<?> dataQuery = cb.createQuery(entityClass);
        Root<?> dataRoot = dataQuery.from(entityClass);
        dataQuery.select(dataRoot);
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

    // --- WRITE OPERATIONS (Task 3) ---
}
