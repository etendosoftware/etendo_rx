/*
 * Copyright 2022-2025  Futit Services SL
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
package com.etendorx.das.unit.repository;

import com.etendorx.das.converter.DynamicDTOConverter;
import com.etendorx.das.metadata.DynamicMetadataService;
import com.etendorx.das.metadata.models.EntityMetadata;
import com.etendorx.das.metadata.models.FieldMappingType;
import com.etendorx.das.metadata.models.FieldMetadata;
import com.etendorx.das.repository.DynamicRepository;
import com.etendorx.das.repository.DynamicRepositoryException;
import com.etendorx.das.repository.EntityClassResolver;
import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseRXObject;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.entities.mapper.lib.PostSyncService;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DynamicRepository.
 *
 * Tests cover:
 * - findById: success, entity not found, metadata not found, entity class resolution
 * - findAll: paginated results, sorting, empty filters
 * - save: exact order of operations (InOrder), pre-instantiation, upsert, no duplicate audit,
 *         double externalId flush, validation id skip, validation failure, no AD_Table usage
 * - saveBatch: single transaction, result count, exception propagation
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DynamicRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private DynamicDTOConverter converter;

    @Mock
    private DynamicMetadataService metadataService;

    @Mock
    private AuditServiceInterceptor auditService;

    @Mock
    private RestCallTransactionHandler transactionHandler;

    @Mock
    private ExternalIdService externalIdService;

    @Mock
    private PostSyncService postSyncService;

    @Mock
    private Validator validator;

    @Mock
    private EntityClassResolver entityClassResolver;

    private DynamicRepository repository;

    // --- Inner test POJO for entity operations ---
    // Needs 'id' property accessible via PropertyUtils (standard getter/setter)
    public static class TestEntity {
        private String id;

        public TestEntity() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @BeforeEach
    void setUp() {
        repository = new DynamicRepository(
            entityManager,
            converter,
            metadataService,
            auditService,
            transactionHandler,
            externalIdService,
            postSyncService,
            validator,
            entityClassResolver,
            Optional.empty() // DefaultValuesHandler
        );
    }

    // --- Helper methods ---

    private FieldMetadata createFieldMetadata(String name, String property, FieldMappingType type) {
        return new FieldMetadata(
            "field-" + name,       // id
            name,                   // name
            property,               // property
            type,                   // fieldMapping
            false,                  // mandatory
            false,                  // identifiesUnivocally
            10L,                    // line
            null,                   // javaMappingQualifier
            null,                   // constantValue
            null,                   // jsonPath
            null,                   // relatedProjectionEntityId
            false                   // createRelated
        );
    }

    private EntityMetadata createEntityMetadata(String id, String name, List<FieldMetadata> fields) {
        return new EntityMetadata(
            id,
            name,
            "table-" + id,         // tableId
            "EW",                   // mappingType
            false,                  // identity
            true,                   // restEndPoint
            name,                   // externalName
            fields
        );
    }

    /**
     * Sets up common stubs for save operations.
     * Returns a TestEntity that merge() will return.
     */
    private TestEntity setupSaveStubs(EntityMetadata entityMeta, Map<String, Object> dto) {
        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);

        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("generated-id");

        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(mergedEntity);
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        // Fresh read after save
        when(entityManager.find(eq(TestEntity.class), eq("generated-id"))).thenReturn(mergedEntity);
        Map<String, Object> resultMap = new HashMap<>(dto);
        resultMap.put("id", "generated-id");
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(resultMap);

        return mergedEntity;
    }

    // ==========================================
    // findById tests
    // ==========================================

    /**
     * Test: findById returns a converted Map for an existing entity.
     */
    @Test
    void findById_returnsConvertedMap() {
        // Arrange
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", List.of(field));
        TestEntity entity = new TestEntity();
        entity.setId("id1");

        Map<String, Object> expectedMap = Map.of("name", "testValue", "id", "id1");

        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);
        when(entityManager.find(TestEntity.class, "id1")).thenReturn(entity);
        when(converter.convertToMap(entity, entityMeta)).thenReturn(expectedMap);

        // Act
        Map<String, Object> result = repository.findById("id1", "proj", "TestEntity");

        // Assert
        assertEquals(expectedMap, result);
        verify(converter).convertToMap(entity, entityMeta);
    }

    /**
     * Test: findById throws EntityNotFoundException when entity does not exist.
     */
    @Test
    void findById_throwsWhenEntityNotFound() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);
        when(entityManager.find(TestEntity.class, "id1")).thenReturn(null);

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
            () -> repository.findById("id1", "proj", "TestEntity"));
    }

    /**
     * Test: findById throws DynamicRepositoryException when metadata is not found.
     */
    @Test
    void findById_throwsWhenMetadataNotFound() {
        // Arrange
        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DynamicRepositoryException.class,
            () -> repository.findById("id1", "proj", "TestEntity"));
    }

    /**
     * Test: findById resolves entity class from table ID in metadata.
     */
    @Test
    void findById_resolvesEntityClassFromTableId() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        TestEntity entity = new TestEntity();

        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);
        when(entityManager.find(TestEntity.class, "id1")).thenReturn(entity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        // Act
        repository.findById("id1", "proj", "TestEntity");

        // Assert
        verify(entityClassResolver).resolveByTableId("table-e1");
    }

    // ==========================================
    // findAll tests
    // ==========================================

    @SuppressWarnings("unchecked")
    private void setupCriteriaBuilderMocks(CriteriaBuilder cb, Class<?> entityClass,
                                            long total, List<?> results) {
        // Count query
        CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
        Root<?> countRoot = mock(Root.class);
        when(cb.createQuery(Long.class)).thenReturn(countQuery);
        when(countQuery.from(entityClass)).thenReturn(countRoot);
        when(countQuery.select(any())).thenReturn(countQuery);

        TypedQuery<Long> countTypedQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(countQuery)).thenReturn((TypedQuery) countTypedQuery);
        when(countTypedQuery.getSingleResult()).thenReturn(total);

        // Data query
        CriteriaQuery<Object> dataQuery = mock(CriteriaQuery.class);
        Root<Object> dataRoot = mock(Root.class);
        when(cb.createQuery(entityClass)).thenReturn((CriteriaQuery) dataQuery);
        when(dataQuery.from(entityClass)).thenReturn((Root) dataRoot);
        when(dataQuery.select(any())).thenReturn((CriteriaQuery) dataQuery);

        // Sorting support -- return mock Path for any property
        Path<?> sortPath = mock(Path.class);
        when(dataRoot.get(anyString())).thenReturn((Path) sortPath);
        // Return a mock Order for both asc and desc
        Order mockOrder = mock(Order.class);
        when(cb.asc(any())).thenReturn(mockOrder);
        when(cb.desc(any())).thenReturn(mockOrder);

        TypedQuery<Object> dataTypedQuery = mock(TypedQuery.class);
        when(entityManager.createQuery(dataQuery)).thenReturn((TypedQuery) dataTypedQuery);
        when(dataTypedQuery.setFirstResult(anyInt())).thenReturn(dataTypedQuery);
        when(dataTypedQuery.setMaxResults(anyInt())).thenReturn(dataTypedQuery);
        when(dataTypedQuery.getResultList()).thenReturn((List) results);
    }

    /**
     * Test: findAll returns paginated results with correct total.
     */
    @Test
    void findAll_returnsPaginatedResults() {
        // Arrange
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", List.of(field));

        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);

        TestEntity entity1 = new TestEntity();
        entity1.setId("1");
        TestEntity entity2 = new TestEntity();
        entity2.setId("2");
        setupCriteriaBuilderMocks(cb, TestEntity.class, 2L, List.of(entity1, entity2));

        when(converter.convertToMap(any(), any(EntityMetadata.class)))
            .thenReturn(Map.of("name", "val1"))
            .thenReturn(Map.of("name", "val2"));

        // Act
        Page<Map<String, Object>> result = repository.findAll("proj", "TestEntity",
            Collections.emptyMap(), PageRequest.of(0, 10));

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    /**
     * Test: findAll applies sorting when Sort is provided.
     */
    @Test
    void findAll_appliesSorting() {
        // Arrange
        FieldMetadata field = createFieldMetadata("name", "nameProperty", FieldMappingType.DIRECT_MAPPING);
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", List.of(field));

        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        setupCriteriaBuilderMocks(cb, TestEntity.class, 0L, Collections.emptyList());

        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

        // Act
        repository.findAll("proj", "TestEntity", Collections.emptyMap(), pageable);

        // Assert - verify cb.asc() was called for sorting
        verify(cb, atLeastOnce()).asc(any());
    }

    /**
     * Test: findAll with empty filters does not add predicates.
     */
    @Test
    void findAll_withEmptyFilters() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());

        when(metadataService.getProjectionEntity("proj", "TestEntity"))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId("table-e1")).thenReturn(TestEntity.class);

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        setupCriteriaBuilderMocks(cb, TestEntity.class, 0L, Collections.emptyList());

        // Act
        repository.findAll("proj", "TestEntity", Collections.emptyMap(), PageRequest.of(0, 10));

        // Assert - cb.equal should never be called (no filter predicates)
        verify(cb, never()).equal(any(), any());
    }

    // ==========================================
    // save/update tests
    // ==========================================

    /**
     * CRITICAL TEST: save follows the exact order of operations.
     * The InOrder verification does NOT include auditService because
     * DynamicRepository does NOT call auditService directly (converter handles it).
     */
    @Test
    void save_followsExactOrderOfOperations() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");

        TestEntity mergedEntity = setupSaveStubs(entityMeta, dto);

        InOrder inOrder = inOrder(transactionHandler, converter, validator,
            entityManager, externalIdService, postSyncService);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert - exact order:
        // 1. begin transaction
        inOrder.verify(transactionHandler).begin();
        // 2. convert DTO to entity (audit handled inside converter)
        inOrder.verify(converter).convertToEntity(eq(dto), any(), eq(entityMeta), eq(entityMeta.fields()));
        // 3. validate
        inOrder.verify(validator).validate(any());
        // 4. first merge
        inOrder.verify(entityManager).merge(any());
        // 5. first flush
        inOrder.verify(entityManager).flush();
        // 6. externalIdService.add
        inOrder.verify(externalIdService).add(eq("table-e1"), any(), any());
        // 7. first externalIdService.flush
        inOrder.verify(externalIdService).flush();
        // 8. second merge
        inOrder.verify(entityManager).merge(any());
        // 9. postSyncService.flush
        inOrder.verify(postSyncService).flush();
        // 10. second externalIdService.flush
        inOrder.verify(externalIdService).flush();
        // 11. commit transaction
        inOrder.verify(transactionHandler).commit();
    }

    /**
     * Test: save pre-instantiates new entity via metamodel (no null passed to converter).
     * Verifies converter.convertToEntity receives a NON-NULL entity argument.
     * Also verifies auditService.setAuditValues is NEVER called directly by the repository.
     */
    @Test
    void save_preInstantiatesNewEntityViaMetamodel() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "new entity");
        // No "id" in DTO -> new entity

        setupSaveStubs(entityMeta, dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Object> entityCaptor = ArgumentCaptor.forClass(Object.class);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert - converter receives non-null entity (pre-instantiated)
        verify(converter).convertToEntity(eq(dto), entityCaptor.capture(), eq(entityMeta), eq(entityMeta.fields()));
        Object capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity, "Converter must receive a non-null entity (pre-instantiated via metamodel)");
        assertTrue(capturedEntity instanceof TestEntity,
            "Pre-instantiated entity should be of the resolved class type");

        // Assert - auditService.setAuditValues is NEVER called by repository
        verify(auditService, never()).setAuditValues(any(BaseRXObject.class));
    }

    /**
     * Test: save checks existence by ID when DTO has "id".
     * When entity exists, converter receives the existing entity.
     */
    @Test
    void save_upsertChecksExistenceById() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", "existing-id");
        dto.put("name", "updated");

        TestEntity existingEntity = new TestEntity();
        existingEntity.setId("existing-id");

        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);
        when(entityManager.find(TestEntity.class, "existing-id")).thenReturn(existingEntity);
        when(entityManager.merge(any())).thenReturn(existingEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(existingEntity);
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        ArgumentCaptor<Object> entityCaptor = ArgumentCaptor.forClass(Object.class);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert - converter receives the existing entity
        verify(converter).convertToEntity(eq(dto), entityCaptor.capture(), eq(entityMeta), eq(entityMeta.fields()));
        assertSame(existingEntity, entityCaptor.getValue(),
            "When entity exists in DB, converter should receive the existing entity");
    }

    /**
     * Test: save creates new instance when DTO has "id" but entity is not found in DB.
     * The pre-instantiated entity should NOT be null.
     */
    @Test
    void save_createsNewInstanceWhenIdNotFoundInDb() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", "new-id");
        dto.put("name", "new entity");

        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);
        // Entity not found in DB
        when(entityManager.find(TestEntity.class, "new-id")).thenReturn(null);

        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("new-id");
        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(mergedEntity);
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        // Fresh read after save
        when(entityManager.find(eq(TestEntity.class), eq("new-id"))).thenReturn(mergedEntity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        ArgumentCaptor<Object> entityCaptor = ArgumentCaptor.forClass(Object.class);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert - converter receives a non-null pre-instantiated entity
        verify(converter).convertToEntity(eq(dto), entityCaptor.capture(), eq(entityMeta), eq(entityMeta.fields()));
        assertNotNull(entityCaptor.getValue(),
            "Even when ID is provided but not found in DB, converter must receive a non-null entity");
    }

    /**
     * Test: save does NOT call auditService.setAuditValues() directly.
     * The converter handles audit internally.
     */
    @Test
    void save_doesNotCallAuditServiceDirectly() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");
        setupSaveStubs(entityMeta, dto);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert
        verify(auditService, never()).setAuditValues(any(BaseRXObject.class));
    }

    /**
     * Test: save calls externalIdService.flush() exactly twice.
     */
    @Test
    void save_callsExternalIdFlushTwice() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");
        setupSaveStubs(entityMeta, dto);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert
        verify(externalIdService, times(2)).flush();
    }

    /**
     * Test: validation skips violations on "id" property.
     * When the only violation is for "id", save should succeed without throwing.
     */
    @SuppressWarnings("unchecked")
    @Test
    void save_validationSkipsIdProperty() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");

        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);

        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("generated-id");
        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(mergedEntity);
        when(entityManager.find(eq(TestEntity.class), eq("generated-id"))).thenReturn(mergedEntity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        // Mock a violation on "id" property
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("id");
        when(violation.getPropertyPath()).thenReturn(path);
        when(validator.validate(any())).thenReturn(Set.of(violation));

        // Act & Assert - should NOT throw (id violation is skipped)
        assertDoesNotThrow(() -> repository.save(dto, "proj", "TestEntity"));
    }

    /**
     * Test: validation throws ResponseStatusException for non-id property violations.
     */
    @SuppressWarnings("unchecked")
    @Test
    void save_validationThrowsForNonIdViolation() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");

        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);

        TestEntity entity = new TestEntity();
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(entity);

        // Mock a violation on "name" property
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        when(validator.validate(any())).thenReturn(Set.of(violation));

        // Act & Assert
        assertThrows(ResponseStatusException.class,
            () -> repository.save(dto, "proj", "TestEntity"));
    }

    /**
     * Negative test: save never uses AD_Table/javaClassName JPQL queries for entity instantiation.
     * Entity resolution is done via EntityClassResolver, not via EntityManager queries.
     */
    @Test
    void save_neverUsesAdTableForInstantiation() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        Map<String, Object> dto = new HashMap<>();
        dto.put("name", "test");
        setupSaveStubs(entityMeta, dto);

        // Act
        repository.save(dto, "proj", "TestEntity");

        // Assert - no JPQL/HQL query containing ADTable or javaClassName
        verify(entityManager, never()).createQuery(anyString());
        verify(entityManager, never()).createQuery(anyString(), any(Class.class));
    }

    // ==========================================
    // saveBatch tests
    // ==========================================

    /**
     * Test: saveBatch processes all entities in a single transaction.
     * transactionHandler.begin() and commit() are each called exactly once.
     */
    @Test
    void saveBatch_processesAllInSingleTransaction() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("batch-id");
        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(mergedEntity);
        when(entityManager.find(eq(TestEntity.class), eq("batch-id"))).thenReturn(mergedEntity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        Map<String, Object> dto1 = Map.of("name", "first");
        Map<String, Object> dto2 = Map.of("name", "second");
        Map<String, Object> dto3 = Map.of("name", "third");

        // Act
        repository.saveBatch(List.of(dto1, dto2, dto3), "proj", "TestEntity");

        // Assert
        verify(transactionHandler, times(1)).begin();
        verify(transactionHandler, times(1)).commit();
        // 2 merges per entity (first save + second save) x 3 entities = 6 total
        verify(entityManager, times(6)).merge(any());
    }

    /**
     * Test: saveBatch returns a result for each DTO in the batch.
     */
    @Test
    void saveBatch_returnsResultForEachDto() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);
        when(validator.validate(any())).thenReturn(Collections.emptySet());

        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("batch-id");
        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(converter.convertToEntity(any(), any(), any(), anyList())).thenReturn(mergedEntity);
        when(entityManager.find(eq(TestEntity.class), eq("batch-id"))).thenReturn(mergedEntity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of("id", "batch-id"));

        Map<String, Object> dto1 = Map.of("name", "first");
        Map<String, Object> dto2 = Map.of("name", "second");

        // Act
        List<Map<String, Object>> results = repository.saveBatch(List.of(dto1, dto2), "proj", "TestEntity");

        // Assert
        assertEquals(2, results.size());
    }

    /**
     * Test: saveBatch does NOT commit when an exception occurs mid-batch.
     * The converter throws on the second DTO processing.
     */
    @Test
    void saveBatch_propagatesExceptionWithoutCommit() {
        // Arrange
        EntityMetadata entityMeta = createEntityMetadata("e1", "TestEntity", Collections.emptyList());
        when(metadataService.getProjectionEntity(anyString(), anyString()))
            .thenReturn(Optional.of(entityMeta));
        when(entityClassResolver.resolveByTableId(entityMeta.tableId()))
            .thenReturn(TestEntity.class);

        // First DTO succeeds
        TestEntity mergedEntity = new TestEntity();
        mergedEntity.setId("first-id");
        when(converter.convertToEntity(any(), any(), any(), anyList()))
            .thenReturn(mergedEntity)
            .thenThrow(new RuntimeException("Conversion failed on second DTO"));
        when(entityManager.merge(any())).thenReturn(mergedEntity);
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(entityManager.find(eq(TestEntity.class), eq("first-id"))).thenReturn(mergedEntity);
        when(converter.convertToMap(any(), any(EntityMetadata.class))).thenReturn(Map.of());

        Map<String, Object> dto1 = Map.of("name", "first");
        Map<String, Object> dto2 = Map.of("name", "second");

        // Act & Assert
        assertThrows(ResponseStatusException.class,
            () -> repository.saveBatch(List.of(dto1, dto2), "proj", "TestEntity"));

        // Verify commit was NEVER called
        verify(transactionHandler, never()).commit();
    }
}
