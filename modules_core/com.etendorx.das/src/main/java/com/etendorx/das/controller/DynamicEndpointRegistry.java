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

import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.ProjectionMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Manages dynamic endpoint registration and validation.
 * At startup, logs all registered dynamic endpoints for visibility.
 * Provides runtime validation for REST endpoint access and entity resolution.
 *
 * Used by DynamicRestController to validate requests and resolve entity metadata
 * from URL path parameters.
 */
@Component
@Slf4j
public class DynamicEndpointRegistry {

    private final DynamicMetadataService metadataService;

    public DynamicEndpointRegistry(DynamicMetadataService metadataService) {
        this.metadataService = metadataService;
    }

    /**
     * Logs all dynamic endpoints at application startup.
     * Iterates all projections and their entities, logging which ones
     * have REST endpoints enabled and which are skipped.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logDynamicEndpoints() {
        Set<String> projectionNames = metadataService.getAllProjectionNames();
        int totalEndpoints = 0;
        int projectionCount = 0;

        for (String projectionName : projectionNames) {
            ProjectionMetadata projection = metadataService.getProjection(projectionName)
                .orElse(null);
            if (projection == null) {
                continue;
            }

            projectionCount++;

            for (EntityMetadata entity : projection.entities()) {
                String displayName = entity.externalName() != null
                    ? entity.externalName()
                    : entity.name();

                if (entity.restEndPoint()) {
                    if (projection.moduleInDevelopment()) {
                        log.info("[X-Ray] Dynamic endpoint: /{}/{} (module: {}, dev: true)",
                            projectionName.toLowerCase(), displayName,
                            projection.moduleName());
                    } else {
                        log.info("Dynamic endpoint registered: /{}/{}",
                            projectionName.toLowerCase(), displayName);
                    }
                    totalEndpoints++;
                } else {
                    log.debug("Skipping REST endpoint for: {}/{} (restEndPoint=false)",
                        projectionName, displayName);
                }
            }
        }

        log.info("Dynamic endpoints: {} endpoints registered across {} projections",
            totalEndpoints, projectionCount);
    }

    /**
     * Checks whether a given entity within a projection is configured as a REST endpoint.
     *
     * @param projectionName      the projection name (case-insensitive, converted to uppercase)
     * @param entityExternalName  the external name of the entity to check
     * @return true if the entity exists and has restEndPoint=true, false otherwise
     */
    public boolean isRestEndpoint(String projectionName, String entityExternalName) {
        ProjectionMetadata projection = metadataService.getProjection(
            projectionName).orElse(null);
        if (projection == null) {
            return false;
        }

        for (EntityMetadata entity : projection.entities()) {
            String matchName = entity.externalName() != null
                ? entity.externalName()
                : entity.name();
            if (matchName.equals(entityExternalName)) {
                return entity.restEndPoint();
            }
        }

        return false;
    }

    /**
     * Resolves an entity within a projection by its external name.
     * Matches against externalName if available, falls back to entity name.
     *
     * @param projectionName      the projection name (matched as-is against cache keys)
     * @param entityExternalName  the external name of the entity to resolve
     * @return Optional containing the entity metadata if found, empty otherwise
     */
    public Optional<EntityMetadata> resolveEntityByExternalName(String projectionName,
                                                                  String entityExternalName) {
        ProjectionMetadata projection = metadataService.getProjection(
            projectionName).orElse(null);
        if (projection == null) {
            return Optional.empty();
        }

        for (EntityMetadata entity : projection.entities()) {
            if (entity.externalName() != null && entity.externalName().equals(entityExternalName)) {
                return Optional.of(entity);
            }
            if (entity.externalName() == null && entity.name().equals(entityExternalName)) {
                return Optional.of(entity);
            }
        }

        return Optional.empty();
    }
}
