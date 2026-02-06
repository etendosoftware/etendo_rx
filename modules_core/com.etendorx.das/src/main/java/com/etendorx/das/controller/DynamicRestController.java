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
package com.etendorx.das.controller;

import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.repository.DynamicRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Single generic REST controller that handles all CRUD operations for dynamically-served
 * projections. Replaces all per-entity generated REST controllers with a single controller
 * that resolves metadata at request time and delegates to {@link DynamicRepository}.
 *
 * <p>Path variables:
 * <ul>
 *   <li>{projectionName} - the projection (e.g., "ETHW")</li>
 *   <li>{entityName} - the entity external name within the projection (e.g., "Product")</li>
 * </ul>
 *
 * <p>This controller replicates the exact behavior of
 * {@link com.etendorx.entities.mapper.lib.BindedRestController} but operates on
 * {@code Map<String, Object>} instead of typed DTOs.
 */
@RestController
@RequestMapping("/{projectionName}/{entityName}")
@Slf4j
public class DynamicRestController {

    private final DynamicRepository repository;
    private final DynamicEndpointRegistry endpointRegistry;
    private final ExternalIdTranslationService externalIdTranslationService;

    public DynamicRestController(DynamicRepository repository,
                                  DynamicEndpointRegistry endpointRegistry,
                                  ExternalIdTranslationService externalIdTranslationService) {
        this.repository = repository;
        this.endpointRegistry = endpointRegistry;
        this.externalIdTranslationService = externalIdTranslationService;
    }

    /**
     * Resolves and validates entity metadata for the given projection and entity name.
     *
     * @param projectionName the projection name from the URL path
     * @param entityName     the entity external name from the URL path
     * @return the resolved EntityMetadata
     * @throws ResponseStatusException with NOT_FOUND if entity not found or REST endpoint disabled
     */
    private EntityMetadata resolveEntityMetadata(String projectionName, String entityName) {
        EntityMetadata entityMeta = endpointRegistry
            .resolveEntityByExternalName(projectionName, entityName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Entity not found: " + entityName + " in projection: " + projectionName));

        if (!entityMeta.restEndPoint()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "REST endpoint not enabled for: " + entityName);
        }

        return entityMeta;
    }

    /**
     * Lists all entities with pagination, sorting, and optional filtering.
     * Pagination params (page, size, sort) are stripped from the filter map.
     * Only DIRECT_MAPPING fields are supported for filtering.
     *
     * @param projectionName the projection name
     * @param entityName     the entity external name
     * @param pageable       pagination and sorting (default size=20)
     * @param allParams      all query parameters (filters extracted after removing pagination)
     * @return a page of entity maps
     */
    @GetMapping
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public Page<Map<String, Object>> findAll(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Map<String, String> allParams) {
        log.debug("GET /{}/{} - findAll with pageable: {}", projectionName, entityName, pageable);

        EntityMetadata entityMeta = resolveEntityMetadata(projectionName, entityName);

        // Strip pagination params from filters
        Map<String, String> filters = new HashMap<>(allParams != null ? allParams : Map.of());
        filters.keySet().removeAll(Arrays.asList("page", "size", "sort"));

        return repository.findAll(
            projectionName.toUpperCase(), entityMeta.name(), filters, pageable);
    }

    /**
     * Retrieves a single entity by its ID.
     *
     * @param projectionName the projection name
     * @param entityName     the entity external name
     * @param id             the entity ID
     * @return the entity map with HTTP 200
     * @throws ResponseStatusException with NOT_FOUND if entity not found
     */
    @GetMapping("/{id}")
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @PathVariable String id) {
        log.debug("GET /{}/{}/{} - findById", projectionName, entityName, id);

        EntityMetadata entityMeta = resolveEntityMetadata(projectionName, entityName);

        try {
            Map<String, Object> result = repository.findById(
                id, projectionName.toUpperCase(), entityMeta.name());
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }
    }
}
