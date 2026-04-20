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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;

/**
 * Service for accessing nested properties on Java beans using dot notation.
 * Wraps Apache Commons BeanUtils to provide null-safe property access.
 */
@Component
@Slf4j
public class PropertyAccessorService {

    /**
     * Gets a nested property value from a bean using dot notation (e.g., "user.role.name").
     * Returns null if any intermediate object is null or if the property doesn't exist.
     *
     * @param bean the object to read from
     * @param propertyPath the dot-notation property path
     * @return the property value, or null if not accessible
     */
    public Object getNestedProperty(Object bean, String propertyPath) {
        if (bean == null || propertyPath == null) {
            return null;
        }

        try {
            return PropertyUtils.getNestedProperty(bean, propertyPath);
        } catch (Exception e) {
            // This is expected behavior when intermediate objects are null
            // (e.g., entity.role is null when reading entity.role.id)
            log.debug("Could not access property '{}' on {}: {}",
                propertyPath, bean.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Sets a nested property value on a bean using dot notation (e.g., "user.role.name").
     * Throws ConversionException if the property cannot be set.
     *
     * @param bean the object to write to
     * @param propertyPath the dot-notation property path
     * @param value the value to set
     * @throws ConversionException if the property cannot be set
     */
    public void setNestedProperty(Object bean, String propertyPath, Object value) {
        if (bean == null || propertyPath == null) {
            return;
        }

        try {
            PropertyUtils.setNestedProperty(bean, propertyPath, value);
        } catch (Exception e) {
            throw new ConversionException(
                "Cannot set property: " + propertyPath + " on " + bean.getClass().getSimpleName(), e);
        }
    }
}
