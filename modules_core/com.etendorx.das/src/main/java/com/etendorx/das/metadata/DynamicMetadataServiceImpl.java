package com.etendorx.das.metadata;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.ETRXProjection;
import com.etendoerp.etendorx.data.ETRXProjectionEntity;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.das.metadata.models.ProjectionMetadata;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of DynamicMetadataService that loads projection metadata from the database
 * using JPA queries and caches the results using Caffeine.
 *
 * This service:
 * - Loads metadata from etrx_projection, etrx_projection_entity, etrx_entity_field tables
 * - Converts JPA entities to immutable record types
 * - Caches metadata for fast lookup
 * - Preloads all projections at startup
 * - Provides cache invalidation when metadata changes
 */
@Service
@Slf4j
public class DynamicMetadataServiceImpl implements DynamicMetadataService {

    private static final String CACHE_NAME_PROJECTIONS_BY_NAME = "projectionsByName";

    private final EntityManager entityManager;
    private final CacheManager cacheManager;

    public DynamicMetadataServiceImpl(EntityManager entityManager, CacheManager cacheManager) {
        this.entityManager = entityManager;
        this.cacheManager = cacheManager;
    }

    /**
     * Preloads all projection metadata into the cache at application startup.
     * This ensures the first request doesn't experience a cold start delay.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void preloadCache() {
        log.info("Preloading projection metadata cache...");

        try {
            List<ETRXProjection> projections = loadProjectionsFromDatabase();
            Cache cache = getCacheByName(CACHE_NAME_PROJECTIONS_BY_NAME);
            if (cache == null) {
                return;
            }

            processAndCacheProjections(projections, cache);
            log.info("Projection metadata cache preloaded successfully with {} entries", projections.size());
        } catch (Exception e) {
            log.error("Failed to preload projection metadata cache", e);
        }
    }

    private List<ETRXProjection> loadProjectionsFromDatabase() {
        String jpql = "SELECT DISTINCT p FROM ETRX_Projection p " +
                     "LEFT JOIN FETCH p.eTRXProjectionEntityList " +
                     "JOIN FETCH p.module m WHERE m.inDevelopment = true";
        List<ETRXProjection> projections = entityManager
            .createQuery(jpql, ETRXProjection.class).getResultList();

        loadEntityFieldsForProjections(projections);
        log.info("Found {} projections to preload", projections.size());
        return projections;
    }

    private void loadEntityFieldsForProjections(List<ETRXProjection> projections) {
        entityManager.createQuery(
            "SELECT DISTINCT pe FROM ETRX_Projection_Entity pe " +
            "LEFT JOIN FETCH pe.eTRXEntityFieldList " +
            "WHERE pe.projection IN :projections", ETRXProjectionEntity.class)
            .setParameter("projections", projections)
            .getResultList();
    }

    private Cache getCacheByName(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("{} cache not found, skipping preload", cacheName);
        }
        return cache;
    }

    private void processAndCacheProjections(List<ETRXProjection> projections, Cache cache) {
        for (ETRXProjection projection : projections) {
            processProjection(projection, cache);
        }
    }

    private void processProjection(ETRXProjection projection, Cache cache) {
        try {
            initializeProjectionRelationships(projection);
            ProjectionMetadata metadata = toProjectionMetadata(projection);
            cache.put(projection.getName(), metadata);
            log.debug("Preloaded projection: {}", projection.getName());
        } catch (Exception e) {
            log.error("Failed to preload projection: {}", projection.getName(), e);
        }
    }

    private void initializeProjectionRelationships(ETRXProjection projection) {
        Hibernate.initialize(projection.getETRXProjectionEntityList());

        if (projection.getETRXProjectionEntityList() != null) {
            for (ETRXProjectionEntity entity : projection.getETRXProjectionEntityList()) {
                initializeEntityRelationships(entity);
            }
        }
    }

    private void initializeEntityRelationships(ETRXProjectionEntity entity) {
        Hibernate.initialize(entity.getETRXEntityFieldList());

        if (entity.getETRXEntityFieldList() != null) {
            for (ETRXEntityField field : entity.getETRXEntityFieldList()) {
                initializeFieldRelationships(field);
            }
        }
    }

    private void initializeFieldRelationships(ETRXEntityField field) {
        if (field.getEtrxProjectionEntityRelated() != null) {
            Hibernate.initialize(field.getEtrxProjectionEntityRelated());
        }
        if (field.getJavaMapping() != null) {
            Hibernate.initialize(field.getJavaMapping());
        }
        if (field.getEtrxConstantValue() != null) {
            Hibernate.initialize(field.getEtrxConstantValue());
        }
    }

    /**
     * Retrieves complete projection metadata by name from cache or database.
     * Results are cached for subsequent lookups.
     */
    @Override
    @Cacheable(value = "projectionsByName", key = "#name")
    public Optional<ProjectionMetadata> getProjection(String name) {
        log.debug("Loading projection from database: {}", name);

        try {
            Optional<ETRXProjection> projectionOpt = loadProjectionByName(name);
            if (projectionOpt.isEmpty()) {
                log.debug("Projection not found: {}", name);
                return Optional.empty();
            }

            ETRXProjection projection = projectionOpt.get();
            loadFieldsForProjection(projection);
            initializeProjectionRelationships(projection);

            ProjectionMetadata metadata = toProjectionMetadata(projection);
            return Optional.of(metadata);
        } catch (Exception e) {
            log.error("Failed to load projection: {}", name, e);
            return Optional.empty();
        }
    }

    private Optional<ETRXProjection> loadProjectionByName(String name) {
        String jpql = "SELECT DISTINCT p FROM ETRX_Projection p " +
                     "LEFT JOIN FETCH p.eTRXProjectionEntityList " +
                     "JOIN FETCH p.module m " +
                     "WHERE p.name = :name AND m.inDevelopment = true";

        TypedQuery<ETRXProjection> query = entityManager.createQuery(jpql, ETRXProjection.class);
        query.setParameter("name", name);
        List<ETRXProjection> results = query.getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private void loadFieldsForProjection(ETRXProjection projection) {
        if (projection.getETRXProjectionEntityList() != null
                && !projection.getETRXProjectionEntityList().isEmpty()) {
            entityManager.createQuery(
                "SELECT DISTINCT pe FROM ETRX_Projection_Entity pe " +
                "LEFT JOIN FETCH pe.eTRXEntityFieldList " +
                "WHERE pe.projection = :projection", ETRXProjectionEntity.class)
                .setParameter("projection", projection)
                .getResultList();
        }
    }

    /**
     * Retrieves a specific entity within a projection.
     * Delegates to getProjection and uses the findEntity helper method.
     */
    @Override
    public Optional<EntityMetadata> getProjectionEntity(String projectionName, String entityName) {
        return getProjection(projectionName)
            .flatMap(projection -> projection.findEntity(entityName));
    }

    /**
     * Retrieves all field mappings for a given projection entity.
     * First attempts to find in cache by iterating cache values.
     * Falls back to database query if not in cache.
     */
    @Override
    public List<FieldMetadata> getFields(String projectionEntityId) {
        if (projectionEntityId == null) {
            return Collections.emptyList();
        }

        Cache cache = cacheManager.getCache(CACHE_NAME_PROJECTIONS_BY_NAME);
        Optional<List<FieldMetadata>> cachedFields = findFieldsInCache(cache, projectionEntityId);
        return cachedFields.orElseGet(() -> loadFieldsFromDb(projectionEntityId));
    }

    private Optional<List<FieldMetadata>> findFieldsInCache(Cache cache, String projectionEntityId) {
        if (cache == null) {
            return Optional.empty();
        }

        Object nativeCache = cache.getNativeCache();
        if (!(nativeCache instanceof com.github.benmanes.caffeine.cache.Cache)) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
            (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;

        for (Object value : caffeineCache.asMap().values()) {
            ProjectionMetadata projection = unwrapCacheValue(value);
            if (projection != null) {
                Optional<List<FieldMetadata>> fields = findFieldsInProjection(projection, projectionEntityId);
                if (fields.isPresent()) {
                    return fields;
                }
            }
        }

        return Optional.empty();
    }

    private Optional<List<FieldMetadata>> findFieldsInProjection(ProjectionMetadata projection, 
                                                                  String projectionEntityId) {
        for (EntityMetadata entity : projection.entities()) {
            if (entity.id().equals(projectionEntityId)) {
                return Optional.of(entity.fields());
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all projection names currently registered in the system.
     * Returns the keySet from the cache, which contains all preloaded projection names.
     */
    @Override
    public Set<String> getAllProjectionNames() {
        Cache cache = cacheManager.getCache(CACHE_NAME_PROJECTIONS_BY_NAME);
        if (cache == null) {
            return Collections.emptySet();
        }

        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
            @SuppressWarnings("unchecked")
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;

            Set<String> names = new HashSet<>();
            for (Object key : caffeineCache.asMap().keySet()) {
                if (key instanceof String) {
                    names.add((String) key);
                }
            }
            return names;
        }

        return Collections.emptySet();
    }

    /**
     * Invalidates the metadata cache, forcing a reload from the database on next access.
     * This should be called when projection metadata is modified.
     */
    @Override
    @CacheEvict(value = "projectionsByName", allEntries = true)
    public void invalidateCache() {
        log.info("Projection metadata cache invalidated");
    }

    /**
     * Converts a JPA ETRXProjection entity to an immutable ProjectionMetadata record.
     */
    private ProjectionMetadata toProjectionMetadata(ETRXProjection projection) {
        boolean moduleDev = projection.getModule() != null
            && Boolean.TRUE.equals(projection.getModule().getInDevelopment());
        String moduleName = projection.getModule() != null
            ? projection.getModule().getName() : null;

        List<EntityMetadata> entities = new ArrayList<>();

        if (projection.getETRXProjectionEntityList() != null) {
            for (ETRXProjectionEntity entity : projection.getETRXProjectionEntityList()) {
                entities.add(toEntityMetadata(entity, moduleDev));
            }
        }

        return new ProjectionMetadata(
            projection.getId(),
            projection.getName(),
            projection.getDescription(),
            projection.getGRPC() != null && projection.getGRPC(),
            entities,
            moduleName,
            moduleDev
        );
    }

    /**
     * Converts a JPA ETRXProjectionEntity to an immutable EntityMetadata record.
     */
    private EntityMetadata toEntityMetadata(ETRXProjectionEntity entity, boolean moduleInDevelopment) {
        List<FieldMetadata> fields = new ArrayList<>();

        if (entity.getETRXEntityFieldList() != null) {
            for (ETRXEntityField field : entity.getETRXEntityFieldList()) {
                fields.add(toFieldMetadata(field));
            }
            // Sort fields by line number
            fields.sort((f1, f2) -> {
                if (f1.line() == null) return 1;
                if (f2.line() == null) return -1;
                return f1.line().compareTo(f2.line());
            });
        }

        return new EntityMetadata(
            entity.getId(),
            entity.getName(),
            entity.getTableEntity() != null ? entity.getTableEntity().getId() : null,
            entity.getMappingType(),
            entity.getIdentity() != null && entity.getIdentity(),
            entity.getRestEndPoint() != null && entity.getRestEndPoint(),
            entity.getExternalName(),
            fields,
            moduleInDevelopment
        );
    }

    /**
     * Converts a JPA ETRXEntityField to an immutable FieldMetadata record.
     */
    private FieldMetadata toFieldMetadata(ETRXEntityField field) {
        FieldMappingType mappingType = FieldMappingType.DIRECT_MAPPING;
        if (field.getFieldMapping() != null) {
            try {
                mappingType = FieldMappingType.fromCode(field.getFieldMapping());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown field mapping type '{}' for field {}, defaulting to DIRECT_MAPPING",
                    field.getFieldMapping(), field.getId());
            }
        }

        String javaMappingQualifier = null;
        if (field.getJavaMapping() != null) {
            javaMappingQualifier = field.getJavaMapping().getQualifier();
        }

        String constantValue = null;
        if (field.getEtrxConstantValue() != null) {
            constantValue = field.getEtrxConstantValue().getDefaultValue();
        }

        String relatedEntityId = null;
        if (field.getEtrxProjectionEntityRelated() != null) {
            relatedEntityId = field.getEtrxProjectionEntityRelated().getId();
        }

        return new FieldMetadata(
            field.getId(),
            field.getName(),
            field.getProperty(),
            mappingType,
            field.getIsmandatory() != null && field.getIsmandatory(),
            field.getIdentifiesUnivocally() != null && field.getIdentifiesUnivocally(),
            field.getLine(),
            javaMappingQualifier,
            constantValue,
            field.getJsonpath(),
            relatedEntityId,
            field.getCreateRelated() != null && field.getCreateRelated()
        );
    }

    /**
     * Unwraps a cache value which may be wrapped by Spring Cache infrastructure.
     */
    private ProjectionMetadata unwrapCacheValue(Object value) {
        if (value instanceof ProjectionMetadata) {
            return (ProjectionMetadata) value;
        }
        // Handle potential cache value wrappers
        if (value != null && value.getClass().getName().contains("CacheValue")) {
            try {
                // Try to access wrapped value via reflection
                var method = value.getClass().getMethod("get");
                Object unwrapped = method.invoke(value);
                if (unwrapped instanceof ProjectionMetadata) {
                    return (ProjectionMetadata) unwrapped;
                }
            } catch (Exception e) {
                log.debug("Could not unwrap cache value", e);
            }
        }
        return null;
    }

    /**
     * Loads fields from database when not found in cache.
     */
    private List<FieldMetadata> loadFieldsFromDb(String projectionEntityId) {
        try {
            String jpql = "SELECT f FROM ETRX_Entity_Field f " +
                         "WHERE f.etrxProjectionEntity.id = :entityId " +
                         "ORDER BY f.line";

            TypedQuery<ETRXEntityField> query = entityManager.createQuery(jpql, ETRXEntityField.class);
            query.setParameter("entityId", projectionEntityId);

            List<ETRXEntityField> fields = query.getResultList();

            // Initialize lazy relationships
            for (ETRXEntityField field : fields) {
                if (field.getEtrxProjectionEntityRelated() != null) {
                    Hibernate.initialize(field.getEtrxProjectionEntityRelated());
                }
                if (field.getJavaMapping() != null) {
                    Hibernate.initialize(field.getJavaMapping());
                }
                if (field.getEtrxConstantValue() != null) {
                    Hibernate.initialize(field.getEtrxConstantValue());
                }
            }

            List<FieldMetadata> result = new ArrayList<>();
            for (ETRXEntityField field : fields) {
                result.add(toFieldMetadata(field));
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to load fields for projection entity: {}", projectionEntityId, e);
            return Collections.emptyList();
        }
    }
}
