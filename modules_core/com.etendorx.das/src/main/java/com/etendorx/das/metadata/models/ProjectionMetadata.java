package com.etendorx.das.metadata.models;

import java.util.List;
import java.util.Optional;

/**
 * Immutable metadata record representing a complete projection configuration.
 * A projection defines how database entities are mapped to DTOs and exposed via APIs.
 *
 * @param id          Unique identifier for this projection
 * @param name        Projection name (used for lookups)
 * @param description Human-readable description
 * @param grpc        Whether this projection supports gRPC endpoints
 * @param entities    List of entities included in this projection
 */
public record ProjectionMetadata(
    String id,
    String name,
    String description,
    boolean grpc,
    List<EntityMetadata> entities
) {
    /**
     * Finds an entity within this projection by name.
     *
     * @param entityName the name of the entity to find
     * @return Optional containing the entity if found, empty otherwise
     */
    public Optional<EntityMetadata> findEntity(String entityName) {
        return entities.stream()
            .filter(e -> e.name().equals(entityName))
            .findFirst();
    }
}
