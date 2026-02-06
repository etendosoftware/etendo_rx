# ROADMAP.md

## Milestone 1: Dynamic DAS Core

### Phase 1: Dynamic Metadata Service
**Goal:** Load and cache etrx_* projection/entity/field metadata at runtime, providing a query API for other components.

**Requirements covered:** FR-1, FR-8

**Plans:** 3 plans

Plans:
- [x] 01-01-PLAN.md -- Models, dependencies, and DynamicMetadataService interface
- [x] 01-02-PLAN.md -- Cache config and DynamicMetadataServiceImpl implementation
- [x] 01-03-PLAN.md -- Unit tests for DynamicMetadataService

**Deliverables:**
- `DynamicMetadataService` that reads `etrx_projection`, `etrx_projection_entity`, `etrx_entity_field` from DB
- In-memory cache with `ProjectionMetadata`, `EntityFieldMetadata` models
- API: `getProjection(name)`, `getProjectionEntity(projectionName, entityName)`, `getFields(projectionEntityId)`
- Support all field mapping types (DM, JM, CV, JP)
- Cache invalidation method (manual trigger)
- Unit tests with mock repositories

**Success criteria:**
- Can load all existing projections from DB at startup
- Field metadata correctly represents all mapping types
- Cache serves repeated lookups without DB queries
- Tests cover: loading, caching, cache miss, invalid projection name

---

### Phase 2: Generic DTO Converter
**Goal:** Convert between JPA entities and `Map<String, Object>` using runtime metadata from Phase 1.

**Requirements covered:** FR-2, FR-6

**Plans:** 3 plans

Plans:
- [x] 02-01-PLAN.md -- Foundation: strategy interface, PropertyAccessor, ConversionContext, simple strategies (DM, CV, CM)
- [x] 02-02-PLAN.md -- Complex strategies (EM, JM, JP) and DynamicDTOConverter orchestrator
- [x] 02-03-PLAN.md -- Unit tests for converter and strategies

**Deliverables:**
- `DynamicDTOConverter` implementing bidirectional conversion
- Entity -> Map (read): iterate fields from metadata, extract values via reflection/property access
- Map -> Entity (write): iterate fields from metadata, set values on entity via reflection/property access
- Type coercion handlers (String, Number, Date, Boolean, reference entities)
- Null safety matching `MappingUtils.handleBaseObject()` behavior
- Support for related entity resolution (lookup by ID for foreign keys)
- Audit field integration via `AuditServiceInterceptor`
- Unit tests with real JPA entities and mock metadata

**Success criteria:**
- Can convert any JPA entity to Map using its projection metadata
- Can populate any JPA entity from Map with correct types
- Related entities resolved by ID from database
- Null values handled consistently with generated converters
- Tests cover: simple fields, references, nulls, type coercion, dates

---

### Phase 3: Generic Repository Layer
**Goal:** Dynamic repository using EntityManager directly for CRUD + pagination + batch, with exact transaction orchestration matching generated repos.

**Requirements covered:** FR-4, FR-5, FR-7

**Plans:** 2 plans

Plans:
- [x] 03-01-PLAN.md -- EntityClassResolver, DynamicRepositoryException, and DynamicRepository with full CRUD/batch/pagination
- [x] 03-02-PLAN.md -- Unit tests for EntityClassResolver and DynamicRepository

**Deliverables:**
- `EntityClassResolver` resolving entity classes via Hibernate metamodel at startup
- `DynamicRepository` with findById, findAll (pagination + filtering), save (upsert), update, saveBatch
- Transaction management via `RestCallTransactionHandler` (manual begin/commit for writes)
- External ID integration (`ExternalIdService.add()`, `flush()` called twice per save)
- Jakarta Validator integration with "id" property skip
- CriteriaBuilder-based dynamic field filtering on DIRECT_MAPPING fields
- Unit tests with mocked dependencies verifying exact order of operations

**Success criteria:**
- CRUD operations work end-to-end with dynamic conversion
- Transaction boundaries match generated repository behavior exactly
- External IDs registered after merge, flushed twice per save
- Validation errors returned for missing mandatory fields (skipping "id")
- Tests cover: create, read, update, list, upsert, validation failure, batch

---

### Phase 4: Generic REST Controller & Endpoint Registration
**Goal:** Single REST controller that dynamically serves all projections, with endpoint registration matching existing URL patterns.

**Requirements covered:** FR-3, NFR-1, NFR-2, NFR-3, NFR-4

**Deliverables:**
- `DynamicRestController` handling `/{prefix}/{entityName}/**` routes
- GET `/` - list with pagination (delegates to DynamicDASRepository.findAll)
- GET `/{id}` - get by ID (delegates to DynamicDASRepository.findById)
- POST `/` - create entity/entities, support `json_path` parameter
- PUT `/{id}` - update entity
- JSON response format compatible with existing generated DTOs
- Request routing: resolve projection + entity from URL path
- Batch POST support (array of entities)
- Integration with existing JWT authentication (no changes to Edge)
- Logging of dynamic endpoint access
- OpenAPI documentation via SpringDoc annotations
- Integration tests for all CRUD endpoints

**Success criteria:**
- Dynamic endpoints accessible at same URL patterns as generated ones
- Response JSON structure identical to generated DTO output
- JWT authentication works without changes
- Pagination, sorting work correctly
- Batch POST creates multiple entities
- Tests cover: GET list, GET by ID, POST single, POST batch, PUT, 404, validation errors

---

### Phase 5: Coexistence & Migration Support
**Goal:** Ensure dynamic and generated endpoints coexist, provide toggle mechanism, and validate equivalence.

**Requirements covered:** NFR-2, NFR-5

**Deliverables:**
- Configuration property to enable/disable dynamic endpoints per projection
- Fallback: if dynamic endpoint fails, log and optionally delegate to generated
- Comparison tool/test: call both dynamic and generated endpoints, diff responses
- Documentation: how to migrate a projection from generated to dynamic
- Performance benchmark: dynamic vs generated endpoint response times
- Integration test suite validating feature parity

**Success criteria:**
- Both dynamic and generated endpoints serve simultaneously without conflicts
- Configuration toggles work correctly
- Response comparison shows identical output for same inputs
- Performance within 20% of generated endpoints
- Migration path documented and tested for at least one projection

---

## Phase Dependencies
```
Phase 1 (Metadata)
    -> Phase 2 (Converter)
        -> Phase 3 (Repository)
            -> Phase 4 (Controller)
                -> Phase 5 (Coexistence)
```

All phases are sequential - each builds on the previous.
