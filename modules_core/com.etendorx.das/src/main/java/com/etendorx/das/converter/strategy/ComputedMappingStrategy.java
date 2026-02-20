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
import org.springframework.stereotype.Component;

/**
 * Strategy for CONSTANT_MAPPING (CM) field types.
 * CM is functionally identical to CV (Constant Value) in the current codebase.
 * Delegates all operations to ConstantValueStrategy.
 */
@Component
public class ComputedMappingStrategy implements FieldConversionStrategy {

    private final ConstantValueStrategy constantValueStrategy;

    public ComputedMappingStrategy(ConstantValueStrategy constantValueStrategy) {
        this.constantValueStrategy = constantValueStrategy;
    }

    @Override
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        return constantValueStrategy.readField(entity, field, ctx);
    }

    @Override
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        constantValueStrategy.writeField(entity, value, field, ctx);
    }
}
