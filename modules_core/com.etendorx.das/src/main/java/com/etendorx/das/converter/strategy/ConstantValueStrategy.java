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
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.entities.entities.mappings.MappingUtils;
import org.springframework.stereotype.Component;

/**
 * Strategy for CONSTANT_VALUE (CV) field types.
 * Reads constant values from the database via MappingUtils.constantValue().
 * Write operations are no-ops since constants are read-only.
 */
@Component
public class ConstantValueStrategy implements FieldConversionStrategy {

    private final MappingUtils mappingUtils;

    public ConstantValueStrategy(MappingUtils mappingUtils) {
        this.mappingUtils = mappingUtils;
    }

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        if (field.constantValue() == null) {
            return null;
        }
        return mappingUtils.constantValue(field.constantValue());
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        // No-op: constants are read-only
        // Generated converters never write CV fields
    }
}
