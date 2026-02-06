# Phase 3: Generic Repository Layer - Research

**Researched:** 2026-02-06
**Domain:** Dynamic JPA repository with EntityManager CRUD, pagination, batch operations, and external ID integration
**Confidence:** HIGH

## Summary

This phase creates a `DynamicRepository` class that uses JPA `EntityManager` directly (no Spring Data JPA repositories) to perform CRUD + pagination operations on any entity at runtime. The repository resolves entity classes via Hibernate's metamodel, converts between `Map<String, Object>` and JPA entities using the Phase 2 `DynamicDTOConverter`, validates entities with Jakarta Validator, manages transactions via the existing `RestCallTransactionHandler`, and integrates with `ExternalIdService` for connector ID mapping.

The generated repository (`BaseDTORepositoryDefault`) was thoroughly analyzed and its exact order of operations documented. The critical sequence for save/update is: begin transaction -> upsert check -> convert DTO to entity -> set default values -> set audit values -> validate -> persist -> register external ID -> flush external IDs -> convert list (for one-to-many) -> persist again -> post-sync flush -> flush external IDs again -> commit transaction -> trigger event handlers -> return converted result.

**Primary recommendation:** Build `DynamicRepository` as a `@Component` with method-level `@Transactional` for read operations and manual `RestCallTransactionHandler` begin/commit for write operations (matching the generated repo pattern). Use `EntityManager.getMetamodel()` at startup to build a `tableName -> Class<?>` resolution map, and use JPQL `TypedQuery` with `CriteriaBuilder` for dynamic field filtering with pagination.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jakarta Persistence (JPA) | 3.x | EntityManager, CriteriaBuilder, Metamodel | Already in use via spring-boot-starter-data-jpa |
| Hibernate ORM | 6.x | JPA implementation, metamodel provider | Current ORM in project |
| Spring Boot Starter Validation | 3.2.2 | Jakarta Validator for entity constraint checking | Already in build.gradle |
| Spring Framework TX | 6.x | @Transactional, TransactionTemplate | Already in use for REST controller transactions |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Caffeine | 3.1.8 | Cache for entity class resolution map | Already in build.gradle, used by Phase 1 |
| Spring Data Commons | (Boot 3.1.4) | Pageable, Page, PageImpl, Sort classes | Already on classpath for pagination interface |
| Apache Commons Lang3 | 3.12.0 | StringUtils for string operations | Already in build.gradle |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| EntityManager directly | Spring Data JPA repositories | Context decision locked: EntityManager directly. No generated repo lookup needed |
| CriteriaBuilder for filtering | JPQL string concatenation | CriteriaBuilder is type-safe and prevents SQL injection |
| Metamodel for class resolution | AD_Table.javaClassName | Context decision locked: use metamodel. Avoids dependency on AD_Table data quality |
| PageImpl for results | Custom Page implementation | PageImpl is the standard Spring Data implementation, already on classpath |

**Installation:**
```bash
# No new dependencies needed. All required libraries are already in build.gradle:
# - spring-boot-starter-data-jpa (JPA, Hibernate, EntityManager)
# - spring-boot-starter-validation (Jakarta Validator)
# - caffeine (caching)
# - spring-data-commons (Pageable, Page)
```

## Architecture Patterns

### Recommended Project Structure
```
com.etendorx.das.repository/
├── DynamicRepository.java              # Main repository: CRUD, findAll, batch
├── EntityClassResolver.java            # Metamodel-based table->class resolution
└── DynamicRepositoryException.java     # Domain-specific exceptions
```

### Pattern 1: Entity Class Resolution via Hibernate Metamodel
**What:** At startup or first-access, scan all JPA managed types via `EntityManager.getMetamodel().getEntities()`, extract `@Table(name=...)` annotation from each entity class, and build a `Map<String, Class<?>>` keyed by table name. Also build a secondary map keyed by `TABLE_ID` static field (all generated entities have `public static final String TABLE_ID`).
**When to use:** Whenever the DynamicRepository needs to resolve which JPA entity class to use for a given EntityMetadata.tableId.
**Example:**
```java
// Source: Analyzed from generated entity pattern
// Every generated entity has:
//   @jakarta.persistence.Entity(name = "ADColumn")
//   @jakarta.persistence.Table(name = "ad_column")
//   public static final String TABLE_ID = "101";

@Component
public class EntityClassResolver {
    private final Map<String, Class<?>> tableNameToClass = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tableIdToClass = new ConcurrentHashMap<>();
    private final EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        Metamodel metamodel = entityManager.getMetamodel();
        for (EntityType<?> entityType : metamodel.getEntities()) {
            Class<?> javaType = entityType.getJavaType();
            jakarta.persistence.Table tableAnn = javaType.getAnnotation(jakarta.persistence.Table.class);
            if (tableAnn != null) {
                tableNameToClass.put(tableAnn.name().toLowerCase(), javaType);
            }
            // Also index by TABLE_ID static field
            try {
                java.lang.reflect.Field field = javaType.getDeclaredField("TABLE_ID");
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    String tableId = (String) field.get(null);
                    tableIdToClass.put(tableId, javaType);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // Not all managed types have TABLE_ID
            }
        }
    }

    public Class<?> resolveByTableId(String tableId) {
        return tableIdToClass.get(tableId);
    }
}
```

### Pattern 2: CriteriaBuilder for Dynamic Field Filtering with Pagination
**What:** Build JPQL criteria queries at runtime based on arbitrary `?field=value` query parameters. Use `CriteriaBuilder` to construct `WHERE` predicates dynamically, and apply pagination via `TypedQuery.setFirstResult()` / `setMaxResults()` with a separate count query for `Page`.
**When to use:** `findAll()` with optional field equality filters plus pagination.
**Example:**
```java
// Source: JPA CriteriaBuilder standard pattern
public Page<Map<String, Object>> findAll(Class<?> entityClass, EntityMetadata entityMeta,
                                          Map<String, String> filters, Pageable pageable) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();

    // Count query
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    Root<?> countRoot = countQuery.from(entityClass);
    countQuery.select(cb.count(countRoot));
    List<Predicate> predicates = buildPredicates(cb, countRoot, filters);
    if (!predicates.isEmpty()) {
        countQuery.where(predicates.toArray(new Predicate[0]));
    }
    long total = entityManager.createQuery(countQuery).getSingleResult();

    // Data query
    CriteriaQuery<?> dataQuery = cb.createQuery(entityClass);
    Root<?> dataRoot = dataQuery.from(entityClass);
    dataQuery.select(dataRoot);
    predicates = buildPredicates(cb, dataRoot, filters);
    if (!predicates.isEmpty()) {
        dataQuery.where(predicates.toArray(new Predicate[0]));
    }

    // Sorting
    if (pageable.getSort().isSorted()) {
        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        for (Sort.Order sortOrder : pageable.getSort()) {
            if (sortOrder.isAscending()) {
                orders.add(cb.asc(dataRoot.get(sortOrder.getProperty())));
            } else {
                orders.add(cb.desc(dataRoot.get(sortOrder.getProperty())));
            }
        }
        dataQuery.orderBy(orders);
    }

    TypedQuery<?> typedQuery = entityManager.createQuery(dataQuery);
    typedQuery.setFirstResult((int) pageable.getOffset());
    typedQuery.setMaxResults(pageable.getPageSize());

    List<?> results = typedQuery.getResultList();
    List<Map<String, Object>> converted = results.stream()
        .map(entity -> converter.convertToMap(entity, entityMeta))
        .toList();

    return new PageImpl<>(converted, pageable, total);
}
```

### Pattern 3: Exact Save/Update Order of Operations (from Generated Repos)
**What:** The `BaseDTORepositoryDefault.performSaveOrUpdate()` method defines the exact transaction flow that MUST be replicated.
**When to use:** Every write operation (save, update, upsert, batch).
**Example:**
```java
// Source: BaseDTORepositoryDefault.java lines 124-178 (analyzed directly)
// EXACT sequence for save/update:
//
// 1. transactionHandler.begin()           -- disable DB triggers
// 2. If update or upsert: find existing entity by ID
//    - If found: isNew = false
//    - If not found and this is save: isNew = true
// 3. Convert DTO to entity via converter
//    - converter.convert(dtoEntity, existingEntity)
//    - For DynamicRepository: converter.convertToEntity(dto, existingEntity, metadata, fields)
// 4. setDefaultValues(entity)             -- Optional<DefaultValuesHandler>
// 5. setAuditValues(entity)               -- if BaseRXObject
// 6. validator.validate(entity)           -- Jakarta Validator constraints
//    - Throw ResponseStatusException(BAD_REQUEST) on violation (skip "id" path)
// 7. entity = repository.save(entity)     -- For us: entityManager.merge(entity)
// 8. externalIdService.add(tableId, dtoId, entity)  -- queue external ID
// 9. externalIdService.flush()            -- persist external IDs
// 10. converter.convertList(dto, entity)  -- handle one-to-many relations
// 11. entity = repository.save(entity)    -- second save after list conversion
// 12. postSyncService.flush()             -- post-sync tasks
// 13. externalIdService.flush()           -- second flush
// 14. transactionHandler.commit()         -- re-enable DB triggers
// 15. triggerEventHandlers(entity, isNew) -- Optional<DefaultValuesHandler>
// 16. Return converted result: converter.convert(retriever.get(newId))
```

### Pattern 4: Batch Operations in Single Transaction
**What:** Accept `List<Map<String, Object>>` and process each entity within the same transaction. If any entity fails, the entire batch rolls back.
**When to use:** Connector batch imports.
**Example:**
```java
// Source: Decision from CONTEXT.md + BindedRestController.handleRawData()
public List<Map<String, Object>> saveBatch(List<Map<String, Object>> dtos,
                                            EntityMetadata entityMeta, String projectionName) {
    List<Map<String, Object>> results = new ArrayList<>();
    try {
        transactionHandler.begin();
        for (Map<String, Object> dto : dtos) {
            // Each entity goes through the full save flow
            Map<String, Object> result = saveOrUpdateSingle(dto, entityMeta, projectionName);
            results.add(result);
        }
        transactionHandler.commit();
        return results;
    } catch (Exception e) {
        // Transaction rollback is automatic via @Transactional
        throw e;
    }
}
```

### Anti-Patterns to Avoid
- **Implementing DASRepository interface:** Context decision locked: DynamicRepository has its own API. DASRepository works with `BaseDTOModel` typed parameters, but DynamicRepository uses `Map<String, Object>`.
- **Using Spring Data JPA repositories for persistence:** Context decision locked: EntityManager directly. `entityManager.persist()` for new entities, `entityManager.merge()` for existing.
- **Skipping the second save:** The generated repo does TWO saves -- first for the main entity, then again after `convertList()` processes one-to-many relations. Skipping the second save will lose child entity associations.
- **Calling externalIdService.flush() only once:** Generated repo calls flush TWICE -- once after the first save, again after the second save. This ensures external IDs are persisted for both the main entity and any child entities created by convertList.
- **Manual transaction management without begin/commit pair:** RestCallTransactionHandler.begin() disables DB triggers (PostgreSQL config variable), commit() re-enables them. Missing either causes trigger behavior mismatches.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity class from table ID | Query AD_Table.javaClassName | EntityManager.getMetamodel() + @Table annotation | Metamodel is in-memory, no DB query; matches context decision |
| Pagination response | Custom page class | Spring Data `PageImpl<>` + `Pageable` | Standard interface, already on classpath, compatible with controllers |
| Transaction begin/commit | New transaction handler | `RestCallTransactionHandler` (existing) | Handles PostgreSQL trigger disable/enable correctly |
| Entity validation | Manual field checks | Jakarta `Validator.validate()` + entity `@NotNull` annotations | Generated entities already annotated with `@NotNull`, `@jakarta.validation.constraints.*` |
| Audit field population | Manual setCreatedBy etc. | `AuditServiceInterceptor.setAuditValues()` | Sets all 7 audit fields from UserContext in one call |
| External ID registration | Direct DB insert | `ExternalIdService.add()` + `flush()` | Handles connector lookup, deduplication, ThreadLocal queue, audit |
| Post-sync tasks | Custom hook system | `PostSyncService.flush()` | Existing pattern for tasks that must run after entity save |
| UUID generation | `UUID.randomUUID()` | Let JPA `@GeneratedValue` handle it | Entity uses custom `UUIDGenerator` configured via `@GenericGenerator` |

**Key insight:** The generated repo (`BaseDTORepositoryDefault`) already has 200 lines of carefully orchestrated service calls. DynamicRepository must replicate this exact orchestration but replace the type-specific `DTOConverter<T,E,F>` and `BaseDASRepository<T>` with the dynamic equivalents (`DynamicDTOConverter` and `EntityManager`).

## Common Pitfalls

### Pitfall 1: EntityManager.persist() vs. EntityManager.merge()
**What goes wrong:** Using `persist()` on an entity that already exists throws `EntityExistsException`. Using `merge()` on a truly new entity works but creates a detached copy.
**Why it happens:** The upsert pattern means the entity may or may not exist. `persist()` is only for new entities.
**How to avoid:** Always use `entityManager.merge()` which handles both cases correctly. For new entities, merge() will persist. For existing entities, merge() will update. This matches the generated pattern where `repository.save()` delegates to `SimpleJpaRepository.save()` which uses `merge()` when entity has an ID.
**Warning signs:** `EntityExistsException` on create, detached entity errors on update.

### Pitfall 2: Missing Second Save After convertList
**What goes wrong:** One-to-many child entities (EM collections) are not persisted because the first `merge()` happens before convertList processes them.
**Why it happens:** The generated repo pattern calls `save()` TWICE: once after the main entity fields, then again after `converter.convertList()` adds children.
**How to avoid:** After calling convertList (or equivalent Phase 2 logic for collection fields), call `entityManager.merge()` again.
**Warning signs:** Parent entities saved correctly but child entity associations missing.

### Pitfall 3: ExternalIdService.add() Requires entity.get_identifier() Non-Null
**What goes wrong:** ExternalIdService.flush() silently skips entities where `baseRXObject.get_identifier()` is null.
**Why it happens:** The `storeExternalId()` method (ExternalIdServiceImpl line 112) checks `baseRXObject.get_identifier() == null` and skips. For new entities, `get_identifier()` might be null before the first persist.
**How to avoid:** Call `externalIdService.add()` AFTER the first `entityManager.merge()` (which assigns the ID). The generated repo does exactly this: save first, then add external ID.
**Warning signs:** External IDs not being registered for new entities; no error thrown (silent skip).

### Pitfall 4: CriteriaBuilder Field Filtering on JPA Property Names vs DTO Names
**What goes wrong:** Query parameters come as DTO field names (e.g., `documentNo`) but CriteriaBuilder needs JPA entity property names (which might differ).
**Why it happens:** FieldMetadata has both `name()` (DTO name) and `property()` (entity property path). Filtering must use the entity property path, not the DTO name.
**How to avoid:** When building filter predicates, map the query param name back to the entity property using FieldMetadata. Only support DIRECT_MAPPING fields for filtering (other types like EM, JM, CV don't have direct entity properties).
**Warning signs:** `IllegalArgumentException: Unable to locate Attribute with the given name` from CriteriaBuilder.

### Pitfall 5: RestCallTransactionHandler.commit() Uses REQUIRES_NEW
**What goes wrong:** The commit() method is annotated with `@Transactional(TxType.REQUIRES_NEW)`, which opens a NEW transaction for the trigger re-enable SQL. If the main transaction is not yet committed, the trigger state can be inconsistent.
**Why it happens:** The handler is designed to be called at the end of the main transaction flow, not inside nested transactions.
**How to avoid:** Call `transactionHandler.commit()` AFTER the main JPA operations but still within the request scope. The write methods should NOT be annotated with `@Transactional` themselves (the generated repo's `performSaveOrUpdate` is NOT `@Transactional` -- it manually manages via begin/commit).
**Warning signs:** Triggers firing during import when they should be disabled; trigger state leaking between requests.

### Pitfall 6: Validator.validate() Rejects New Entities with Null ID
**What goes wrong:** Generated entities have `@NotNull` on the ID field, so `validator.validate()` flags the ID as a violation for new entities where ID is null before persist.
**Why it happens:** The `@Id` field has `@NotNull` annotation, but JPA generates the ID during persist.
**How to avoid:** The generated repo explicitly skips violations on the `"id"` property path (BaseDTORepositoryDefault line 150). Replicate this filter: `if (!StringUtils.equals(violation.getPropertyPath().toString(), "id"))`.
**Warning signs:** Every new entity save fails validation with "id: must not be null".

### Pitfall 7: Table Name Case Sensitivity in Metamodel Lookup
**What goes wrong:** EntityMetadata.tableId stores the AD_Table primary key (e.g., "259"), not the SQL table name (e.g., "c_order"). The metamodel indexes by table name from `@Table` annotation.
**Why it happens:** There are two different identifiers: the AD_Table UUID/ID and the SQL table name.
**How to avoid:** Build BOTH indexes in EntityClassResolver: one by `TABLE_ID` static field (matches EntityMetadata.tableId) and one by `@Table(name)` annotation (matches SQL table name). The primary lookup for DynamicRepository should use TABLE_ID since EntityMetadata.tableId is an AD_Table ID.
**Warning signs:** Entity class not found, null entity type, ClassNotFoundException.

## Code Examples

Verified patterns from official sources:

### DynamicRepository Core Structure
```java
// Source: Modeled after BaseDTORepositoryDefault.java (analyzed directly)
@Component
@Slf4j
public class DynamicRepository {
    private final EntityManager entityManager;
    private final DynamicDTOConverter converter;
    private final DynamicMetadataService metadataService;
    private final AuditServiceInterceptor auditService;
    private final RestCallTransactionHandler transactionHandler;
    private final ExternalIdService externalIdService;
    private final PostSyncService postSyncService;
    private final Validator validator;
    private final EntityClassResolver entityClassResolver;

    // Read operations use @Transactional
    @Transactional
    public Page<Map<String, Object>> findAll(String projectionName, String entityName,
                                              Map<String, String> filters, Pageable pageable) {
        // 1. Resolve metadata
        EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
            .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityName));
        // 2. Resolve entity class
        Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());
        // 3. Build criteria query with filters and pagination
        // 4. Convert results via converter.convertToMap()
        // 5. Return PageImpl
    }

    // Write operations use manual transaction handler (NOT @Transactional)
    public Map<String, Object> save(Map<String, Object> dto, String projectionName,
                                     String entityName) {
        // Replicates BaseDTORepositoryDefault.performSaveOrUpdate() exactly
    }
}
```

### findById with EntityManager
```java
// Source: EntityManager standard API + generated retriever pattern
@Transactional
public Map<String, Object> findById(String id, String projectionName, String entityName) {
    EntityMetadata entityMeta = metadataService.getProjectionEntity(projectionName, entityName)
        .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityName));
    Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());

    Object entity = entityManager.find(entityClass, id);
    if (entity == null) {
        throw new jakarta.persistence.EntityNotFoundException(
            "Entity " + entityName + " not found with id: " + id);
    }
    return converter.convertToMap(entity, entityMeta);
}
```

### Upsert Pattern (Matching Generated Repo)
```java
// Source: BaseDTORepositoryDefault.performSaveOrUpdate() lines 124-179
private Map<String, Object> performSaveOrUpdate(Map<String, Object> dto,
        EntityMetadata entityMeta, boolean isNew) {
    String newId;
    try {
        transactionHandler.begin();

        Class<?> entityClass = entityClassResolver.resolveByTableId(entityMeta.tableId());
        Object existingEntity = null;

        String dtoId = (String) dto.get("id");

        // Upsert: always check existence when ID provided
        if (dtoId != null) {
            existingEntity = entityManager.find(entityClass, dtoId);
            if (existingEntity != null) {
                isNew = false;
            }
        }

        // Convert DTO to entity
        Object entity = converter.convertToEntity(dto, existingEntity, entityMeta, entityMeta.fields());
        if (entity == null) {
            throw new IllegalStateException("Entity conversion failed");
        }

        // Audit values
        if (entity instanceof BaseRXObject rxObj) {
            auditService.setAuditValues(rxObj);
        }

        // Validate (skip "id" violations)
        validateEntity(entity);

        // First save
        entity = entityManager.merge(entity);
        entityManager.flush();

        // External ID registration
        String tableId = entityMeta.tableId();
        externalIdService.add(tableId, dtoId, entity);
        externalIdService.flush();

        // Second save (after any list processing)
        entity = entityManager.merge(entity);
        postSyncService.flush();
        externalIdService.flush();

        transactionHandler.commit();

        // Return freshly read result
        newId = getEntityId(entity);
        Object freshEntity = entityManager.find(entityClass, newId);
        return converter.convertToMap(freshEntity, entityMeta);
    } catch (ResponseStatusException e) {
        throw e;
    } catch (Exception e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}
```

### Entity Validation (Matching Generated Pattern)
```java
// Source: BaseDTORepositoryDefault lines 145-158
private void validateEntity(Object entity) {
    Set<ConstraintViolation<Object>> violations = validator.validate(entity);
    if (!violations.isEmpty()) {
        List<String> messages = new ArrayList<>();
        boolean hasViolations = false;
        for (ConstraintViolation<Object> violation : violations) {
            // Skip "id" path -- JPA generates ID, so it's null before persist
            if (!StringUtils.equals(violation.getPropertyPath().toString(), "id")) {
                messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
                hasViolations = true;
            }
        }
        if (hasViolations) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Validation failed: " + messages);
        }
    }
}
```

### Dynamic Field Filtering with CriteriaBuilder
```java
// Source: Standard JPA CriteriaBuilder pattern
private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<?> root,
                                         Map<String, String> filters,
                                         List<FieldMetadata> fields) {
    List<Predicate> predicates = new ArrayList<>();
    for (Map.Entry<String, String> filter : filters.entrySet()) {
        String dtoFieldName = filter.getKey();
        String value = filter.getValue();

        // Find matching DIRECT_MAPPING field
        FieldMetadata field = fields.stream()
            .filter(f -> f.name().equals(dtoFieldName)
                && f.fieldMapping() == FieldMappingType.DIRECT_MAPPING)
            .findFirst()
            .orElse(null);

        if (field != null) {
            // Use entity property path for the predicate
            String entityProperty = field.property();
            // Handle nested properties (e.g., "organization.id")
            Path<?> path = buildPath(root, entityProperty);
            predicates.add(cb.equal(path, value));
        }
    }
    return predicates;
}

private Path<?> buildPath(Root<?> root, String propertyPath) {
    String[] parts = propertyPath.split("\\.");
    Path<?> path = root;
    for (String part : parts) {
        path = path.get(part);
    }
    return path;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Generated *DASRepository per entity | Single DynamicRepository | This project (Phase 3) | Eliminates code generation for repository layer |
| Generated retriever (JsonPathEntityRetriever) | EntityManager.find() directly | This project (Phase 3) | No need for generated retriever class per entity |
| Type-safe DTOConverter<T,E,F> | DynamicDTOConverter with Map<String,Object> | Phase 2 | Repository receives/returns Map instead of typed DTOs |
| Spring Data JPA repository.save() | EntityManager.merge() directly | Context decision | Removes dependency on generated JPA repository interfaces |
| AD_Table.javaClassName for class lookup | Hibernate metamodel + @Table annotation | Context decision | In-memory lookup, no DB query needed |

**Deprecated/outdated:**
- `BaseDASRepository<T>` extends `JpaRepository`: Not used by DynamicRepository, which uses EntityManager directly
- `JsonPathEntityRetriever<T>`: Generated retriever class. DynamicRepository uses EntityManager.find() instead
- `DTOConverter<T,E,F>` interface: Type-parameterized interface. DynamicRepository uses DynamicDTOConverter which works with Object/Map

## Decisions for Claude's Discretion

### Pagination Approach: Use Spring Pageable/Page

**Recommendation:** Use `org.springframework.data.domain.Pageable` and `org.springframework.data.domain.PageImpl` from Spring Data Commons.

**Rationale:**
- Already on classpath via spring-boot-starter-data-jpa
- Compatible with `@PageableDefault(size = 20)` annotations in Phase 4 controllers
- Matches the existing `BindedRestController.findAll(Pageable)` signature exactly
- `PageImpl` provides standard JSON serialization with `content`, `totalElements`, `totalPages`, `number`, `size` fields
- No custom code needed for pagination metadata

### Error Format for Validation Failures

**Recommendation:** Use `ResponseStatusException` with `HttpStatus.BAD_REQUEST` and a message listing violations, matching the existing pattern in `BaseDTORepositoryDefault`.

**Rationale:**
- The generated repo already uses `ResponseStatusException(BAD_REQUEST, "Validation failed: " + messages)` (line 156)
- Controller layer catches `ResponseStatusException` and returns appropriate HTTP status
- Consistent with existing error handling patterns in `BindedRestController`
- Format: `"Validation failed: [fieldPath: message, fieldPath2: message2]"`

### Internal Class Structure

**Recommendation:** Three classes:

1. **`DynamicRepository`** -- Main repository class with all CRUD + batch methods. Single `@Component`, injected into controller in Phase 4.
2. **`EntityClassResolver`** -- Standalone `@Component` responsible for metamodel scanning and class resolution. Initialized at startup. Reusable across the application.
3. **`DynamicRepositoryException`** -- Custom runtime exception for repository-specific errors (entity not found, resolution failures). Controller maps to appropriate HTTP status.

**Rationale:**
- EntityClassResolver has a distinct lifecycle (startup scan, cached lookups) separate from per-request CRUD logic
- Separating it allows Phase 4 controller and future components to also resolve entity classes
- DynamicRepositoryException provides a clean exception hierarchy distinct from ConversionException (Phase 2)

## Open Questions

Things that couldn't be fully resolved:

1. **convertList() Equivalent for DynamicDTOConverter**
   - What we know: Generated repo calls `converter.convertList(dtoEntity, entity)` after first save to handle one-to-many child entities
   - What's unclear: DynamicDTOConverter from Phase 2 does not have a `convertList()` method. Phase 2 focused on single-entity conversion.
   - Recommendation: For Phase 3, handle only single-entity fields in save. If the DTO contains nested collections (EM fields with List type), skip them in the first pass. The `convertToEntity()` already handles EM fields for many-to-one (setting a reference). One-to-many collection handling may need a follow-up enhancement.
   - Impact: LOW -- batch import typically works on flat entities. Complex one-to-many hierarchies are less common in connector imports.

2. **DefaultValuesHandler Integration**
   - What we know: Generated repo uses `Optional<DefaultValuesHandler>` to set default values and trigger event handlers. This is an optional dependency.
   - What's unclear: Whether any DefaultValuesHandler implementations exist in the codebase for specific entities. The interface is generic (takes `Object entity`).
   - Recommendation: Inject `Optional<DefaultValuesHandler>` in DynamicRepository to match the generated pattern. Call `setDefaultValues()` if present. This is a safety net -- most entities may not have a handler.

3. **PostSyncService Tasks in Dynamic Context**
   - What we know: PostSyncService collects `Runnable` tasks and flushes them after save. Generated repos call `postSyncService.flush()` after the second save.
   - What's unclear: Whether any code adds tasks to PostSyncService in the dynamic DAS flow (tasks would be added by generated converters, which we're replacing).
   - Recommendation: Still call `postSyncService.flush()` for forward compatibility. It's a no-op if no tasks were queued, but ensures any future integration works.

4. **Entity ID Extraction After Merge**
   - What we know: After `entityManager.merge(entity)`, the returned entity has its generated ID. But getting it requires knowing which field is the `@Id` field.
   - What's unclear: Whether all entities consistently use `getId()` method name or if some have different ID accessor names.
   - Recommendation: All generated entities observed have `java.lang.String id` field with `getId()` method. Use reflection or `BaseSerializableObject.get_identifier()` (which returns `_id` from `@Formula` annotation -- this is NOT the same as the primary key). Instead, access the `id` field via BeanUtils `PropertyUtils.getProperty(entity, "id")`.

## Sources

### Primary (HIGH confidence)
- Direct codebase analysis (files read and analyzed in this research):
  - `BaseDTORepositoryDefault.java` -- Complete save/update/findAll flow (lines 37-217)
  - `RestCallTransactionHandler.java` / `RestCallTransactionHandlerImpl.java` -- Transaction begin/commit interface and implementation
  - `ExternalIdService.java` / `ExternalIdServiceImpl.java` -- External ID add/flush/convertExternalToInternalId
  - `AuditServiceInterceptorImpl.java` -- Audit field population logic (lines 51-74)
  - `PostSyncServiceImpl.java` -- Post-sync task queue and flush
  - `DynamicDTOConverter.java` -- Phase 2 converter with convertToMap/convertToEntity
  - `DynamicMetadataService.java` -- Phase 1 metadata service interface
  - `EntityMetadata.java`, `FieldMetadata.java`, `ProjectionMetadata.java` -- Phase 1 metadata records
  - `BindedRestController.java` -- Existing REST controller pattern with pagination
  - Generated entity classes (Order.java, Column.java) -- `@Table`, `TABLE_ID`, entity annotation patterns
  - `BaseRXObject.java` -- Base entity class with audit fields
  - `BaseSerializableObject.java` -- Interface with `getTableId()`, `get_identifier()`
  - `DASRepository.java` -- Existing repository interface (NOT implemented by DynamicRepository)
  - `build.gradle` -- Dependency verification

### Secondary (MEDIUM confidence)
- [JPA Metamodel API](https://www.objectdb.com/java/jpa/persistence/metamodel) -- Metamodel entity iteration
- [Mapping Entity Class Names to SQL Table Names with JPA | Baeldung](https://www.baeldung.com/jpa-entity-table-names) -- @Table annotation access
- [How to get entity mapping metadata from Hibernate | Vlad Mihalcea](https://vladmihalcea.com/how-to-get-the-entity-mapping-to-database-table-binding-metadata-from-hibernate/) -- Hibernate metadata extraction
- [JPA Pagination | Baeldung](https://www.baeldung.com/jpa-pagination) -- CriteriaBuilder + pagination pattern
- [JPA Criteria With Pagination | DZone](https://dzone.com/articles/jpa-criteria-with-pagination) -- Count query + data query pattern

### Tertiary (LOW confidence)
- [Implementing a Generic JPA Filter | Medium](https://medium.com/@sarthakagrawal.work/implementing-a-generic-jpa-filter-with-column-selector-and-pagination-using-jpa-specification-in-305ee77deae1) -- Generic filter approach (verified pattern against JPA spec)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- All libraries already in project dependencies, verified in build.gradle
- Architecture: HIGH -- Patterns derived directly from analyzing BaseDTORepositoryDefault.java source code
- Order of operations: HIGH -- Exact line-by-line analysis of generated repo save/update flow
- Entity class resolution: HIGH -- Verified entity @Table annotations and TABLE_ID static fields across multiple generated entities
- Pitfalls: HIGH -- Identified from actual code analysis (validator skip "id", ExternalIdService null check, double save)
- Pagination: MEDIUM -- CriteriaBuilder pattern is standard JPA, but field name mapping (DTO to entity property) needs runtime testing
- Open questions: MEDIUM -- convertList() gap identified, needs Phase 3 plan to address

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (30 days -- stable domain, Hibernate/Spring Boot versions locked)
