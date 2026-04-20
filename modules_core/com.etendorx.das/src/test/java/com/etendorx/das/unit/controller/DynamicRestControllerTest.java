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
import com.etendorx.das.controller.DynamicRestController;
import com.etendorx.das.controller.ExternalIdTranslationService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.repository.DynamicRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DynamicRestController.
 *
 * Tests cover:
 * - GET list (findAll): paginated results, filter param cleanup, 404 for missing projection, 404 for restEndPoint=false
 * - GET by ID (findById): success 200, 404 not found, 404 restEndPoint=false
 * - POST (create): single 201, batch 201, json_path extraction, empty body 400, default json_path, translateExternalIds called
 * - PUT (update): success 201, id from path, translateExternalIds called
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicRestControllerTest {

    private static final String PROJECTION_NAME = "obmap";
    private static final String ENTITY_NAME = "Product";
    private static final String ENTITY_META_NAME = "ProductEntity";

    @Mock
    private DynamicRepository repository;

    @Mock
    private DynamicEndpointRegistry endpointRegistry;

    @Mock
    private ExternalIdTranslationService externalIdTranslationService;

    private DynamicRestController controller;

    private EntityMetadata testEntityMeta;

    @BeforeEach
    void setUp() {
        controller = new DynamicRestController(repository, endpointRegistry,
            externalIdTranslationService);

        testEntityMeta = new EntityMetadata(
            "entity-1",                 // id
            ENTITY_META_NAME,           // name (internal name used with repository)
            "TABLE-PRODUCT",            // tableId
            "EW",                       // mappingType
            false,                      // identity
            true,                       // restEndPoint
            ENTITY_NAME,                // externalName (used in URL)
            List.of(),                  // fields
            false                       // moduleInDevelopment
        );

        // Default setup: registry resolves the test entity
        when(endpointRegistry.resolveEntityByExternalName(PROJECTION_NAME, ENTITY_NAME))
            .thenReturn(Optional.of(testEntityMeta));
    }

    // ==========================================
    // GET list (findAll) tests
    // ==========================================

    /**
     * Test: findAll returns a page of entities with correct content.
     */
    @Test
    void findAll_returnsPageOfEntities() {
        // Arrange
        Map<String, Object> entity1 = Map.of("name", "Product A");
        Map<String, Object> entity2 = Map.of("name", "Product B");
        Page<Map<String, Object>> page = new PageImpl<>(List.of(entity1, entity2));
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAll(eq(PROJECTION_NAME), eq(ENTITY_META_NAME), anyMap(), eq(pageable)))
            .thenReturn(page);

        // Act
        Page<Map<String, Object>> result = controller.findAll(
            PROJECTION_NAME, ENTITY_NAME, pageable, new HashMap<>());

        // Assert
        assertEquals(2, result.getContent().size());
        verify(repository).findAll(eq(PROJECTION_NAME), eq(ENTITY_META_NAME), anyMap(), eq(pageable));
    }

    /**
     * Test: findAll strips page/size/sort params from allParams before passing to repository.
     * Only custom filter params should reach the repository.
     */
    @Test
    void findAll_removesPageParamsFromFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Map<String, String> allParams = new HashMap<>();
        allParams.put("page", "0");
        allParams.put("size", "20");
        allParams.put("sort", "name,asc");
        allParams.put("name", "test");

        Page<Map<String, Object>> emptyPage = new PageImpl<>(List.of());
        when(repository.findAll(anyString(), anyString(), anyMap(), any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act
        controller.findAll(PROJECTION_NAME, ENTITY_NAME, pageable, allParams);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> filtersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).findAll(eq(PROJECTION_NAME), eq(ENTITY_META_NAME),
            filtersCaptor.capture(), eq(pageable));

        Map<String, String> capturedFilters = filtersCaptor.getValue();
        assertFalse(capturedFilters.containsKey("page"), "page param should be stripped");
        assertFalse(capturedFilters.containsKey("size"), "size param should be stripped");
        assertFalse(capturedFilters.containsKey("sort"), "sort param should be stripped");
        assertEquals("test", capturedFilters.get("name"), "Custom filter should remain");
    }

    /**
     * Test: findAll returns 404 when the projection/entity does not exist.
     */
    @Test
    void findAll_returns404ForNonExistentProjection() {
        // Arrange
        when(endpointRegistry.resolveEntityByExternalName("unknown", "Product"))
            .thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 20);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.findAll("unknown", "Product", pageable, new HashMap<>()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Test: findAll returns 404 when the entity has restEndPoint=false.
     */
    @Test
    void findAll_returns404ForRestEndpointFalse() {
        // Arrange
        EntityMetadata nonRestEntity = new EntityMetadata(
            "entity-2", "InternalEntity", "TABLE-INT", "EW",
            false, false, "Internal", List.of(), false); // restEndPoint=false

        when(endpointRegistry.resolveEntityByExternalName(PROJECTION_NAME, "Internal"))
            .thenReturn(Optional.of(nonRestEntity));
        Pageable pageable = PageRequest.of(0, 20);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.findAll(PROJECTION_NAME, "Internal", pageable, new HashMap<>()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ==========================================
    // GET by ID (findById) tests
    // ==========================================

    /**
     * Test: findById returns entity with HTTP 200.
     */
    @Test
    void findById_returnsEntity() {
        // Arrange
        Map<String, Object> entity = Map.of("id", "123", "name", "Product A");
        when(repository.findById("123", PROJECTION_NAME, ENTITY_META_NAME)).thenReturn(entity);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.findById(
            PROJECTION_NAME, ENTITY_NAME, "123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(entity, response.getBody());
    }

    /**
     * Test: findById returns 404 when entity is not found (EntityNotFoundException).
     */
    @Test
    void findById_returns404WhenNotFound() {
        // Arrange
        when(repository.findById("999", PROJECTION_NAME, ENTITY_META_NAME))
            .thenThrow(new EntityNotFoundException("Not found"));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.findById(PROJECTION_NAME, ENTITY_NAME, "999"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    /**
     * Test: findById returns 404 when the entity has restEndPoint=false.
     */
    @Test
    void findById_returns404ForRestEndpointFalse() {
        // Arrange
        EntityMetadata nonRestEntity = new EntityMetadata(
            "entity-2", "InternalEntity", "TABLE-INT", "EW",
            false, false, "Internal", List.of(), false); // restEndPoint=false

        when(endpointRegistry.resolveEntityByExternalName(PROJECTION_NAME, "Internal"))
            .thenReturn(Optional.of(nonRestEntity));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.findById(PROJECTION_NAME, "Internal", "123"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ==========================================
    // POST (create) tests
    // ==========================================

    /**
     * Test: create returns 201 CREATED for a single entity.
     */
    @Test
    void create_singleEntity_returns201() {
        // Arrange
        String rawJson = "{\"name\":\"Test Product\"}";
        Map<String, Object> savedEntity = Map.of("id", "new-1", "name", "Test Product");
        when(repository.save(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME)))
            .thenReturn(savedEntity);

        // Act
        ResponseEntity<Object> response = controller.create(
            PROJECTION_NAME, ENTITY_NAME, rawJson, null);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(savedEntity, response.getBody());
    }

    /**
     * Test: create calls translateExternalIds before saving.
     */
    @Test
    void create_callsTranslateExternalIds() {
        // Arrange
        String rawJson = "{\"name\":\"Test\",\"organization\":\"EXT-ORG\"}";
        when(repository.save(anyMap(), anyString(), anyString()))
            .thenReturn(Map.of("id", "new-1"));

        // Act
        controller.create(PROJECTION_NAME, ENTITY_NAME, rawJson, null);

        // Assert
        verify(externalIdTranslationService).translateExternalIds(anyMap(), eq(testEntityMeta));
    }

    /**
     * Test: create handles batch creation (JSON array) and returns 201 with list.
     */
    @Test
    void create_batchEntities_returns201() {
        // Arrange
        String rawJson = "[{\"name\":\"A\"},{\"name\":\"B\"}]";
        Map<String, Object> result1 = Map.of("id", "1", "name", "A");
        Map<String, Object> result2 = Map.of("id", "2", "name", "B");
        when(repository.saveBatch(anyList(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME)))
            .thenReturn(List.of(result1, result2));

        // Act
        ResponseEntity<Object> response = controller.create(
            PROJECTION_NAME, ENTITY_NAME, rawJson, null);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> body = (List<Map<String, Object>>) response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
    }

    /**
     * Test: create with json_path extracts nested data before saving.
     */
    @Test
    void create_withJsonPath_extractsNestedData() {
        // Arrange
        String rawJson = "{\"data\":{\"name\":\"Nested Product\"}}";
        Map<String, Object> savedEntity = Map.of("id", "new-1", "name", "Nested Product");
        when(repository.save(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME)))
            .thenReturn(savedEntity);

        // Act
        ResponseEntity<Object> response = controller.create(
            PROJECTION_NAME, ENTITY_NAME, rawJson, "$.data");

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(repository).save(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME));
    }

    /**
     * Test: create returns 400 BAD_REQUEST when body is empty.
     */
    @Test
    void create_emptyBody_returns400() {
        // Arrange & Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.create(PROJECTION_NAME, ENTITY_NAME, "", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    /**
     * Test: create defaults json_path to "$" (whole document) when null.
     * Verifies processing succeeds without explicit json_path.
     */
    @Test
    void create_defaultsJsonPathToDollarSign() {
        // Arrange
        String rawJson = "{\"name\":\"Test\"}";
        when(repository.save(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME)))
            .thenReturn(Map.of("id", "new-1"));

        // Act & Assert - should not throw
        assertDoesNotThrow(() ->
            controller.create(PROJECTION_NAME, ENTITY_NAME, rawJson, null));

        verify(repository).save(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME));
    }

    // ==========================================
    // PUT (update) tests
    // ==========================================

    /**
     * Test: update returns 201 CREATED (matching BindedRestController.put behavior).
     */
    @Test
    void update_returnsUpdatedEntity_with201() {
        // Arrange
        String rawJson = "{\"name\":\"Updated Product\"}";
        Map<String, Object> updatedEntity = Map.of("id", "123", "name", "Updated Product");
        when(repository.update(anyMap(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME)))
            .thenReturn(updatedEntity);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.update(
            PROJECTION_NAME, ENTITY_NAME, "123", rawJson);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(updatedEntity, response.getBody());
    }

    /**
     * Test: update sets the ID from the path variable into the DTO before saving.
     */
    @Test
    void update_setsIdFromPathVariable() {
        // Arrange
        String rawJson = "{\"name\":\"Updated\"}";
        when(repository.update(anyMap(), anyString(), anyString()))
            .thenReturn(Map.of("id", "123", "name", "Updated"));

        // Act
        controller.update(PROJECTION_NAME, ENTITY_NAME, "123", rawJson);

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> dtoCaptor = ArgumentCaptor.forClass(Map.class);
        verify(repository).update(dtoCaptor.capture(), eq(PROJECTION_NAME), eq(ENTITY_META_NAME));

        Map<String, Object> capturedDto = dtoCaptor.getValue();
        assertEquals("123", capturedDto.get("id"),
            "The id from the path variable should be set on the DTO");
    }

    /**
     * Test: update calls translateExternalIds before repository.update.
     */
    @Test
    void update_callsTranslateExternalIds() {
        // Arrange
        String rawJson = "{\"name\":\"Updated\",\"organization\":\"EXT-ORG\"}";
        when(repository.update(anyMap(), anyString(), anyString()))
            .thenReturn(Map.of("id", "123"));

        // Act
        controller.update(PROJECTION_NAME, ENTITY_NAME, "123", rawJson);

        // Assert
        verify(externalIdTranslationService).translateExternalIds(anyMap(), eq(testEntityMeta));
    }
}
