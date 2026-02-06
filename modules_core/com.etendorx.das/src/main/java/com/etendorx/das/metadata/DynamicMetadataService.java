package com.etendorx.das.metadata;

import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.das.metadata.models.ProjectionMetadata;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Public API interface for querying dynamic projection metadata.
 * This service provides cached access to projection configurations loaded from the database.
 *
 * Implementations should cache metadata for performance and provide cache invalidation
 * when metadata is modified in the database.
 */
public interface DynamicMetadataService {

    /**
     * Retrieves complete projection metadata by name.
     *
     * @param name the projection name to look up
     * @return Optional containing the projection metadata if found, empty otherwise
     */
    Optional<ProjectionMetadata> getProjection(String name);

    /**
     * Retrieves a specific entity within a projection.
     *
     * @param projectionName the name of the projection
     * @param entityName     the name of the entity within the projection
     * @return Optional containing the entity metadata if found, empty otherwise
     */
    Optional<EntityMetadata> getProjectionEntity(String projectionName, String entityName);

    /**
     * Retrieves all field mappings for a given projection entity.
     *
     * @param projectionEntityId the unique ID of the projection entity
     * @return List of field metadata, ordered by line number; empty list if entity not found
     */
    List<FieldMetadata> getFields(String projectionEntityId);

    /**
     * Retrieves all projection names currently registered in the system.
     *
     * @return Set of all projection names
     */
    Set<String> getAllProjectionNames();

    /**
     * Invalidates the metadata cache, forcing a reload from the database on next access.
     * This should be called when projection metadata is modified.
     */
    void invalidateCache();
}
