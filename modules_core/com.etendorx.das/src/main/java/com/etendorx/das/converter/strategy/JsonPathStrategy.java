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
package com.etendorx.das.converter.strategy;

import com.etendorx.das.converter.ConversionContext;
import com.etendorx.das.converter.FieldConversionStrategy;
import com.etendorx.das.converter.PropertyAccessorService;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.mappings.MappingUtils;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategy for JSON_PATH (JP) field types.
 * Extracts field values from JSON string properties using JsonPath expressions.
 * Write operations are not supported (read-only strategy).
 */
@Component
@Slf4j
public class JsonPathStrategy implements FieldConversionStrategy {

    private final PropertyAccessorService propertyAccessorService;
    private final MappingUtils mappingUtils;

    public JsonPathStrategy(PropertyAccessorService propertyAccessorService, MappingUtils mappingUtils) {
        this.propertyAccessorService = propertyAccessorService;
        this.mappingUtils = mappingUtils;
    }

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        // Get the source property (a JSON string field on the entity)
        Object rawJson = propertyAccessorService.getNestedProperty(entity, field.property());

        if (rawJson == null) {
            return null;
        }

        String jsonString = rawJson.toString();

        // Get the JsonPath expression
        String path = field.jsonPath();
        if (path == null) {
            // Fall back to direct property value if no JsonPath expression
            return mappingUtils.handleBaseObject(rawJson);
        }

        try {
            Object result = JsonPath.read(jsonString, path);
            if (result != null) {
                return mappingUtils.handleBaseObject(result);
            }
            return null;
        } catch (PathNotFoundException e) {
            log.debug("JsonPath '{}' not found in field '{}': {}", path, field.name(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.debug("Error reading JsonPath '{}' for field '{}': {}", path, field.name(), e.getMessage());
            return null;
        }
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        // JP fields are typically read-only in the generated converters
        // (JsonPath extraction from a JSON column).
        log.warn("JsonPath field write not supported for field: {}", field.name());
    }
}
