package com.etendorx.das.metadata.models;

import java.util.List;

/**
 * Immutable metadata record representing an entity within a projection.
 * Contains the entity configuration and all its field mappings.
 *
 * @param id            Unique identifier for this projection entity
 * @param name          Entity name (used in projection structure)
 * @param tableId       Database table ID this entity maps to
 * @param mappingType   Entity mapping type configuration
 * @param identity      Whether this is the identity/primary entity
 * @param restEndPoint  Whether to expose this entity as a REST endpoint
 * @param externalName  External name for REST API (if different from name)
 * @param fields        List of field mappings for this entity
 */
public record EntityMetadata(
    String id,
    String name,
    String tableId,
    String mappingType,
    boolean identity,
    boolean restEndPoint,
    String externalName,
    List<FieldMetadata> fields
) {}
