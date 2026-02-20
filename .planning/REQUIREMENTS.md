# REQUIREMENTS.md

## Milestone 1: Dynamic DAS Core

### Functional Requirements

**FR-1: Dynamic Entity Metadata Loading**
- Load `etrx_projection`, `etrx_projection_entity`, `etrx_entity_field` at runtime
- Build in-memory metadata model from these tables
- Support all field mapping types: DM (Direct Mapping), JM (Java Mapping), CV (Constant Value), JP (JsonPath)
- Resolve entity relationships (ad_table_id references) dynamically

**FR-2: Generic DTO Conversion**
- Convert JPA Entity -> `Map<String, Object>` (Read DTO) using field metadata
- Convert `Map<String, Object>` (Write DTO) -> JPA Entity using field metadata
- Support nested entity references (resolve related entities by ID)
- Support `identifiesUnivocally` fields for entity lookup
- Handle null values, type coercion, and date formatting
- Support `MappingUtils.handleBaseObject()` equivalent for null safety

**FR-3: Generic REST Controller**
- Single controller that handles all dynamically-registered projections
- URL pattern: `/{mappingPrefix}/{externalName}` matching existing convention
- Support CRUD operations: GET (list with pagination), GET by ID, POST (create), PUT (update)
- Support `json_path` query parameter for nested JSON extraction
- Support batch POST (array of entities)
- Maintain Swagger/OpenAPI documentation

**FR-4: Dynamic Repository Layer**
- Generic repository that wraps existing JPA repositories
- Lookup JPA repository by entity class name at runtime
- Support pagination via Spring Data `Pageable`
- Integrate with `RestCallTransactionHandler` for transaction management
- Support upsert logic (check existence before create)

**FR-5: External ID Integration**
- Maintain existing `ExternalIdService` integration
- Call `add()` and `flush()` during save operations
- Support `convertExternalToInternalId()` for incoming references
- Work with `etrx_instance_connector` and `etrx_instance_externalid` tables

**FR-6: Audit Trail Integration**
- Set audit fields (createdBy, updatedBy, creationDate, updated) via `AuditServiceInterceptor`
- Apply to both new and updated entities

**FR-7: Validation**
- Validate incoming data against `ismandatory` field metadata
- Integrate with Jakarta Validator for entity-level constraints
- Return meaningful error messages on validation failure

**FR-8: Metadata Caching**
- Cache projection/entity/field metadata in memory
- Support cache invalidation (initially manual, later event-driven)
- Minimize database queries for metadata lookups

### Non-Functional Requirements

**NFR-1: Performance**
- Dynamic endpoint response time within 20% of generated equivalent
- Metadata cache hit ratio > 95% under normal operation
- No additional database round-trips for metadata on cached requests

**NFR-2: Backwards Compatibility**
- Existing generated controllers continue to work unchanged
- No modification to JPA entities or repositories
- Coexistence: both generated and dynamic endpoints can serve simultaneously
- Same JSON response format as generated DTOs

**NFR-3: Security**
- Respect existing JWT authentication via Edge gateway
- No new authentication/authorization surface
- Validate all input to prevent injection

**NFR-4: Observability**
- Log dynamic endpoint registration at startup
- Log metadata cache misses
- Error logging consistent with existing patterns (Log4j2)

**NFR-5: Testability**
- Unit tests for generic converter with mock metadata
- Integration tests for dynamic endpoint CRUD operations
- Test coexistence with generated endpoints
