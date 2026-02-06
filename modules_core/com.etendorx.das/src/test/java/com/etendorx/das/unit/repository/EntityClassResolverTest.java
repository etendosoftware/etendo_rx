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
package com.etendorx.das.unit.repository;

import com.etendorx.das.repository.DynamicRepositoryException;
import com.etendorx.das.repository.EntityClassResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityClassResolver.
 *
 * Tests cover metamodel scanning, resolution by table ID, resolution by table name,
 * case insensitivity, not-found exceptions, entities without TABLE_ID,
 * and entities without @Table annotation.
 */
@ExtendWith(MockitoExtension.class)
public class EntityClassResolverTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Metamodel metamodel;

    private EntityClassResolver resolver;

    // --- Inner test entity classes ---

    @Table(name = "test_table")
    static class TestEntity {
        public static final String TABLE_ID = "100";
    }

    @Table(name = "another_table")
    static class AnotherEntity {
        public static final String TABLE_ID = "200";
    }

    @Table(name = "no_tableid")
    static class NoTableIdEntity {
        // No TABLE_ID field
    }

    // Entity without @Table annotation
    static class NoTableAnnotationEntity {
        public static final String TABLE_ID = "300";
    }

    @BeforeEach
    void setUp() {
        resolver = new EntityClassResolver(entityManager);
        when(entityManager.getMetamodel()).thenReturn(metamodel);
    }

    // --- Helper methods ---

    @SuppressWarnings("unchecked")
    private EntityType<?> mockEntityType(Class<?> javaType) {
        EntityType<?> mockType = mock(EntityType.class);
        when(mockType.getJavaType()).thenReturn((Class) javaType);
        return mockType;
    }

    // --- Tests ---

    /**
     * Test: init scans metamodel and populates both maps (table ID and table name).
     * After init, resolveByTableId returns the correct class for both entities.
     */
    @Test
    void init_scansMetamodelAndPopulatesMaps() {
        // Arrange
        EntityType<?> testType = mockEntityType(TestEntity.class);
        EntityType<?> anotherType = mockEntityType(AnotherEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(testType, anotherType));

        // Act
        resolver.init();

        // Assert
        assertEquals(TestEntity.class, resolver.resolveByTableId("100"));
        assertEquals(AnotherEntity.class, resolver.resolveByTableId("200"));
    }

    /**
     * Test: resolveByTableId returns the correct entity class for a known table ID.
     */
    @Test
    void resolveByTableId_returnsCorrectClass() {
        // Arrange
        EntityType<?> testType = mockEntityType(TestEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(testType));
        resolver.init();

        // Act
        Class<?> result = resolver.resolveByTableId("100");

        // Assert
        assertEquals(TestEntity.class, result);
    }

    /**
     * Test: resolveByTableId throws DynamicRepositoryException when table ID is not found.
     */
    @Test
    void resolveByTableId_throwsWhenNotFound() {
        // Arrange
        when(metamodel.getEntities()).thenReturn(Collections.emptySet());
        resolver.init();

        // Act & Assert
        assertThrows(DynamicRepositoryException.class, () -> resolver.resolveByTableId("999"));
    }

    /**
     * Test: resolveByTableName returns the correct entity class for a known table name.
     */
    @Test
    void resolveByTableName_returnsCorrectClass() {
        // Arrange
        EntityType<?> testType = mockEntityType(TestEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(testType));
        resolver.init();

        // Act
        Class<?> result = resolver.resolveByTableName("test_table");

        // Assert
        assertEquals(TestEntity.class, result);
    }

    /**
     * Test: resolveByTableName is case-insensitive (uppercase input resolves correctly).
     */
    @Test
    void resolveByTableName_isCaseInsensitive() {
        // Arrange
        EntityType<?> testType = mockEntityType(TestEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(testType));
        resolver.init();

        // Act
        Class<?> result = resolver.resolveByTableName("TEST_TABLE");

        // Assert
        assertEquals(TestEntity.class, result);
    }

    /**
     * Test: resolveByTableName throws DynamicRepositoryException when table name is not found.
     */
    @Test
    void resolveByTableName_throwsWhenNotFound() {
        // Arrange
        when(metamodel.getEntities()).thenReturn(Collections.emptySet());
        resolver.init();

        // Act & Assert
        assertThrows(DynamicRepositoryException.class, () -> resolver.resolveByTableName("nonexistent"));
    }

    /**
     * Test: init handles entity without TABLE_ID field gracefully.
     * The entity is indexed by table name but not by table ID.
     */
    @Test
    void init_handlesEntityWithoutTableId() {
        // Arrange
        EntityType<?> noIdType = mockEntityType(NoTableIdEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(noIdType));

        // Act
        resolver.init();

        // Assert - indexed by table name
        assertEquals(NoTableIdEntity.class, resolver.resolveByTableName("no_tableid"));
        // Assert - NOT indexed by any table ID
        assertThrows(DynamicRepositoryException.class, () -> resolver.resolveByTableId("no_tableid"));
    }

    /**
     * Test: init handles entity without @Table annotation gracefully.
     * No exception is thrown; the entity is simply skipped for table name indexing.
     */
    @Test
    void init_handlesEntityWithoutTableAnnotation() {
        // Arrange
        EntityType<?> noAnnotationType = mockEntityType(NoTableAnnotationEntity.class);
        when(metamodel.getEntities()).thenReturn(Set.of(noAnnotationType));

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> resolver.init());

        // Entity should still be indexed by TABLE_ID (it has the field)
        assertEquals(NoTableAnnotationEntity.class, resolver.resolveByTableId("300"));
        // But NOT indexed by table name (no @Table annotation)
        assertThrows(DynamicRepositoryException.class,
            () -> resolver.resolveByTableName("NoTableAnnotationEntity"));
    }
}
