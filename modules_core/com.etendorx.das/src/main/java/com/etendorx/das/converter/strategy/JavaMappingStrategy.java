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
import com.etendorx.entities.mapper.lib.DTOReadMapping;
import com.etendorx.entities.mapper.lib.DTOWriteMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Strategy for JAVA_MAPPING (JM) field types.
 * Delegates read operations to DTOReadMapping beans and write operations to DTOWriteMapping beans,
 * resolved by Spring qualifier from the field metadata.
 */
@Component
@Slf4j
public class JavaMappingStrategy implements FieldConversionStrategy {

    private final ApplicationContext applicationContext;

    public JavaMappingStrategy(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object readField(Object entity, FieldMetadata field, ConversionContext ctx) {
        String qualifier = field.javaMappingQualifier();
        if (qualifier == null || qualifier.isBlank()) {
            log.warn("No Java mapping qualifier for field: {}", field.name());
            return null;
        }

        try {
            DTOReadMapping mapper = applicationContext.getBean(qualifier, DTOReadMapping.class);
            return mapper.map(entity);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("DTOReadMapping bean not found for qualifier '{}' on field: {}",
                qualifier, field.name());
            return null;
        } catch (Exception e) {
            log.error("Error executing DTOReadMapping for field {}: {}",
                field.name(), e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeField(Object entity, Object value, FieldMetadata field, ConversionContext ctx) {
        String qualifier = field.javaMappingQualifier();
        if (qualifier == null || qualifier.isBlank()) {
            return;
        }

        try {
            DTOWriteMapping mapper = applicationContext.getBean(qualifier, DTOWriteMapping.class);

            // DTOWriteMapping.map(entity, dto) expects the full DTO object, not a single field value.
            // Get the full DTO from the conversion context.
            Object fullDto = ctx.getFullDto();
            if (fullDto == null) {
                log.warn("Full DTO not available in context for JM write on field: {}", field.name());
                return;
            }

            mapper.map(entity, fullDto);
        } catch (NoSuchBeanDefinitionException e) {
            log.error("DTOWriteMapping bean not found for qualifier '{}' on field: {}",
                qualifier, field.name());
        } catch (Exception e) {
            log.error("Error executing DTOWriteMapping for field {}: {}",
                field.name(), e.getMessage());
        }
    }
}
