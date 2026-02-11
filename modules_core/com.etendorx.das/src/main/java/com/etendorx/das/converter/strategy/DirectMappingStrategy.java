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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Strategy for DIRECT_MAPPING (DM) field types.
 * Reads entity properties using PropertyAccessorService and applies MappingUtils.handleBaseObject().
 * Writes values to entity properties with type coercion for dates and numbers.
 */
@Component
@Slf4j
public class DirectMappingStrategy implements FieldConversionStrategy {

    private final PropertyAccessorService propertyAccessorService;
    private final MappingUtils mappingUtils;

    public DirectMappingStrategy(PropertyAccessorService propertyAccessorService, MappingUtils mappingUtils) {
        this.propertyAccessorService = propertyAccessorService;
        this.mappingUtils = mappingUtils;
    }

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        // Get raw value from entity property
        Object rawValue = propertyAccessorService.getNestedProperty(entity, field.property());

        if (rawValue == null) {
            return null;
        }

        // Apply handleBaseObject to convert BaseSerializableObject to identifier,
        // Date to formatted string, PersistentBag to List
        return mappingUtils.handleBaseObject(rawValue);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        if (value == null) {
            propertyAccessorService.setNestedProperty(entity, field.property(), null);
            return;
        }

        // Handle Date type coercion: if target is Date and value is String, parse it
        try {
            Class<?> targetType = PropertyUtils.getPropertyType(entity, field.property());

            if (targetType != null && Date.class.isAssignableFrom(targetType) && value instanceof String) {
                Date parsedDate = mappingUtils.parseDate((String) value);
                propertyAccessorService.setNestedProperty(entity, field.property(), parsedDate);
                return;
            }

            // Handle numeric coercion
            if (targetType != null && value instanceof Number) {
                if (Long.class.equals(targetType) || long.class.equals(targetType)) {
                    if (value instanceof Integer) {
                        propertyAccessorService.setNestedProperty(entity, field.property(), ((Integer) value).longValue());
                        return;
                    }
                }
                if (BigDecimal.class.equals(targetType)) {
                    if (value instanceof Integer) {
                        propertyAccessorService.setNestedProperty(entity, field.property(), new BigDecimal((Integer) value));
                        return;
                    }
                    if (value instanceof Long) {
                        propertyAccessorService.setNestedProperty(entity, field.property(), new BigDecimal((Long) value));
                        return;
                    }
                    if (value instanceof Double) {
                        propertyAccessorService.setNestedProperty(entity, field.property(), BigDecimal.valueOf((Double) value));
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not determine property type for '{}', setting raw value", field.property());
        }

        // Set value directly
        propertyAccessorService.setNestedProperty(entity, field.property(), value);
    }
}
