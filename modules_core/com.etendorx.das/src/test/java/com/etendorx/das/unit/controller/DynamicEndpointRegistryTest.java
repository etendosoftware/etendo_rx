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

import com.etendorx.das.controller.DynamicEndpointRegistry;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.ProjectionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DynamicEndpointRegistry.
 *
 * Tests cover:
 * - resolveEntityByExternalName: matching by externalName, fallback to name, non-existent entity/projection
 * - isRestEndpoint: true/false/non-existent cases
 * - logDynamicEndpoints: startup logging runs without error
 */
@ExtendWith(MockitoExtension.class)
public class DynamicEndpointRegistryTest {

    @Mock
    private DynamicMetadataService metadataService;

    private DynamicEndpointRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DynamicEndpointRegistry(metadataService);
    }

    // --- Helper methods ---

    private EntityMetadata createEntity(String name, String externalName, boolean restEndPoint) {
        return new EntityMetadata(
            "entity-" + name,           // id
            name,                       // name
            "table-" + name,            // tableId
            "EW",                       // mappingType
            false,                      // identity
            restEndPoint,               // restEndPoint
            externalName,               // externalName
            List.of()                   // fields
        );
    }

    private ProjectionMetadata createProjection(String name, List<EntityMetadata> entities) {
        return new ProjectionMetadata(
            "proj-" + name,             // id
            name,                       // name
            "Test projection",          // description
            false,                      // grpc
            entities
        );
    }

    // ==========================================
    // resolveEntityByExternalName tests
    // ==========================================

    /**
     * Test: resolveEntityByExternalName finds an entity by its externalName field.
     * The projection name is converted to uppercase for the metadataService lookup.
     */
    @Test
    void resolveEntityByExternalName_findsEntityByExternalName() {
        // Arrange
        EntityMetadata entity = createEntity("ProductEntity", "Product", true);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        Optional<EntityMetadata> result = registry.resolveEntityByExternalName("obmap", "Product");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Product", result.get().externalName());
        assertEquals("ProductEntity", result.get().name());
    }

    /**
     * Test: resolveEntityByExternalName falls back to entity name when externalName is null.
     */
    @Test
    void resolveEntityByExternalName_fallsBackToNameWhenExternalNameNull() {
        // Arrange
        EntityMetadata entity = createEntity("Product", null, true);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        Optional<EntityMetadata> result = registry.resolveEntityByExternalName("obmap", "Product");

        // Assert
        assertTrue(result.isPresent());
        assertNull(result.get().externalName());
        assertEquals("Product", result.get().name());
    }

    /**
     * Test: resolveEntityByExternalName returns empty when no entity matches.
     */
    @Test
    void resolveEntityByExternalName_returnsEmptyForNonExistent() {
        // Arrange
        EntityMetadata entity = createEntity("ProductEntity", "Product", true);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        Optional<EntityMetadata> result = registry.resolveEntityByExternalName("obmap", "NonExistent");

        // Assert
        assertTrue(result.isEmpty());
    }

    /**
     * Test: resolveEntityByExternalName returns empty when the projection does not exist.
     */
    @Test
    void resolveEntityByExternalName_returnsEmptyForNonExistentProjection() {
        // Arrange
        when(metadataService.getProjection("UNKNOWN")).thenReturn(Optional.empty());

        // Act
        Optional<EntityMetadata> result = registry.resolveEntityByExternalName("unknown", "Product");

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==========================================
    // isRestEndpoint tests
    // ==========================================

    /**
     * Test: isRestEndpoint returns true when entity has restEndPoint=true.
     */
    @Test
    void isRestEndpoint_returnsTrueForRestEndpoint() {
        // Arrange
        EntityMetadata entity = createEntity("ProductEntity", "Product", true);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        boolean result = registry.isRestEndpoint("obmap", "Product");

        // Assert
        assertTrue(result);
    }

    /**
     * Test: isRestEndpoint returns false when entity has restEndPoint=false.
     */
    @Test
    void isRestEndpoint_returnsFalseForNonRestEndpoint() {
        // Arrange
        EntityMetadata entity = createEntity("InternalEntity", "Internal", false);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        boolean result = registry.isRestEndpoint("obmap", "Internal");

        // Assert
        assertFalse(result);
    }

    /**
     * Test: isRestEndpoint returns false when the entity does not exist in the projection.
     */
    @Test
    void isRestEndpoint_returnsFalseForNonExistentEntity() {
        // Arrange
        EntityMetadata entity = createEntity("ProductEntity", "Product", true);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity));

        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act
        boolean result = registry.isRestEndpoint("obmap", "NonExistent");

        // Assert
        assertFalse(result);
    }

    // ==========================================
    // logDynamicEndpoints tests
    // ==========================================

    /**
     * Test: logDynamicEndpoints executes without error when projections exist.
     * Verifies the startup logging method runs successfully.
     */
    @Test
    void logDynamicEndpoints_logsWithoutError() {
        // Arrange
        EntityMetadata entity1 = createEntity("ProductEntity", "Product", true);
        EntityMetadata entity2 = createEntity("InternalEntity", "Internal", false);
        ProjectionMetadata projection = createProjection("OBMAP", List.of(entity1, entity2));

        when(metadataService.getAllProjectionNames()).thenReturn(Set.of("OBMAP"));
        when(metadataService.getProjection("OBMAP")).thenReturn(Optional.of(projection));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> registry.logDynamicEndpoints());
    }
}
