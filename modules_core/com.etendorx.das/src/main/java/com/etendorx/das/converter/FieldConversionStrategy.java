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

import com.etendorx.das.metadata.models.FieldMetadata;

/**
 * Strategy interface for converting field values between entity and DTO representations.
 * Each field mapping type (DM, JM, CV, JP, EM, CM) has a corresponding implementation.
 */
public interface FieldConversionStrategy {

    /**
     * Reads a field value from an entity based on the field metadata.
     *
     * @param entity the entity to read from
     * @param field the field metadata describing how to read
     * @param ctx the conversion context for cycle detection
     * @return the value to place in the DTO
     */
    Object readField(Object entity, FieldMetadata field, ConversionContext ctx);

    /**
     * Writes a field value to an entity based on the field metadata.
     *
     * @param entity the entity to write to
     * @param value the value from the DTO
     * @param field the field metadata describing how to write
     * @param ctx the conversion context for cycle detection
     */
    void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx);
}
