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
package com.etendorx.das.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves JPA entity classes by table ID or table name using Hibernate's metamodel.
 * At startup, scans all managed entity types and builds lookup maps from:
 * - {@code @jakarta.persistence.Table(name=...)} annotation -> entity class
 * - {@code public static final String TABLE_ID} field -> entity class
 *
 * This replaces the AD_Table.javaClassName JPQL lookup approach with an
 * in-memory metamodel-based resolution that requires no database queries.
 */
@Component
@Slf4j
public class EntityClassResolver {

    private final EntityManager entityManager;
    private final Map<String, Class<?>> tableNameToClass = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tableIdToClass = new ConcurrentHashMap<>();

    public EntityClassResolver(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Initializes the entity class resolution maps by scanning the Hibernate metamodel.
     * Uses {@code @EventListener(ApplicationReadyEvent.class)} to ensure all entity types
     * are registered before scanning (matching Phase 1 startup pattern).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Metamodel metamodel = entityManager.getMetamodel();
        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> javaType = entityType.getJavaType();

            // Index by @Table(name=...) annotation (lowercase for case-insensitive lookup)
            jakarta.persistence.Table tableAnn = javaType.getAnnotation(jakarta.persistence.Table.class);
            if (tableAnn != null && !tableAnn.name().isEmpty()) {
                tableNameToClass.put(tableAnn.name().toLowerCase(), javaType);
            }

            // Index by static TABLE_ID field (all generated entities have this)
            try {
                Field field = javaType.getDeclaredField("TABLE_ID");
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())) {
                    String tableId = (String) field.get(null);
                    if (tableId != null) {
                        tableIdToClass.put(tableId, javaType);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // Not all managed types have TABLE_ID (e.g., framework entities)
            }
        }
        log.info("EntityClassResolver initialized: {} entities by table name, {} entities by table ID",
            tableNameToClass.size(), tableIdToClass.size());
    }

    /**
     * Resolves an entity class by its AD_Table ID.
     *
     * @param tableId the AD_Table primary key (e.g., "259" for c_order)
     * @return the JPA entity class
     * @throws DynamicRepositoryException if no entity is registered for the given table ID
     */
    public Class<?> resolveByTableId(String tableId) {
        Class<?> entityClass = tableIdToClass.get(tableId);
        if (entityClass == null) {
            throw new DynamicRepositoryException(
                "No entity class found for table ID: " + tableId);
        }
        return entityClass;
    }

    /**
     * Resolves an entity class by its SQL table name.
     *
     * @param tableName the SQL table name (case-insensitive, e.g., "c_order")
     * @return the JPA entity class
     * @throws DynamicRepositoryException if no entity is registered for the given table name
     */
    public Class<?> resolveByTableName(String tableName) {
        Class<?> entityClass = tableNameToClass.get(tableName.toLowerCase());
        if (entityClass == null) {
            throw new DynamicRepositoryException(
                "No entity class found for table name: " + tableName);
        }
        return entityClass;
    }
}
