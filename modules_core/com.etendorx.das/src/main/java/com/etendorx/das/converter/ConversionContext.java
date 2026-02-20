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
package com.etendorx.das.converter;

import com.etendorx.entities.entities.BaseRXObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Context object for tracking conversion state to prevent infinite recursion.
 * NOT a Spring component - instantiated per-conversion as new ConversionContext().
 */
public class ConversionContext {

    private final Set<String> visitedEntityKeys = new HashSet<>();
    private Map<String, Object> fullDto;

    /**
     * Checks if an entity has already been visited during conversion.
     * This prevents infinite recursion in circular entity relationships.
     *
     * @param entity the entity to check
     * @return true if the entity was already visited (cycle detected), false otherwise
     */
    public boolean isVisited(Object entity) {
        if (entity == null) {
            return false;
        }

        String key = entity.getClass().getName() + ":" + getEntityId(entity);

        if (visitedEntityKeys.contains(key)) {
            return true;
        }

        visitedEntityKeys.add(key);
        return false;
    }

    /**
     * Extracts a unique identifier for an entity.
     * Uses the entity's identifier if available, otherwise uses identity hash code.
     *
     * @param entity the entity
     * @return the unique identifier string
     */
    private String getEntityId(Object entity) {
        if (entity instanceof BaseRXObject) {
            return ((BaseRXObject) entity).get_identifier();
        }
        return String.valueOf(System.identityHashCode(entity));
    }

    /**
     * Gets the full DTO map. Used by JM write strategies to access the complete DTO.
     *
     * @return the full DTO map, or null if not set
     */
    public Map<String, Object> getFullDto() {
        return fullDto;
    }

    /**
     * Sets the full DTO map.
     *
     * @param fullDto the complete DTO map
     */
    public void setFullDto(Map<String, Object> fullDto) {
        this.fullDto = fullDto;
    }
}
