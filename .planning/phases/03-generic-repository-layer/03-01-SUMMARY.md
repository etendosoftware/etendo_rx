---
phase: 03-generic-repository-layer
plan: 01
subsystem: api
tags: [jpa, hibernate, entitymanager, criteriabuilder, metamodel, pagination, repository, crud, batch]

# Dependency graph
requires:
  - phase: 01-dynamic-metadata-service
    provides: "DynamicMetadataService for projection entity resolution and field metadata"
  - phase: 02-generic-dto-converter
    provides: "DynamicDTOConverter for bidirectional entity-to-map conversion"
provides:
  - "EntityClassResolver for metamodel-based entity class resolution by table ID and table name"
  - "DynamicRepository with findById, findAll (pagination/filtering), save, update, saveBatch"
  - "DynamicRepositoryException for domain-specific repository errors"
affects:
  - 04-generic-rest-controller
  - 05-coexistence-migration

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Hibernate metamodel scanning at startup for entity class resolution"
    - "CriteriaBuilder for dynamic field filtering with DIRECT_MAPPING-only support"
    - "Manual transactionHandler begin/commit for write operations (no @Transactional)"
    - "Pre-instantiation of new entities via EntityClassResolver (bypasses AD_Table.javaClassName)"
    - "Double externalIdService.flush() matching BaseDTORepositoryDefault pattern"
    - "Jakarta Validator with id property skip for new entities"

key-files:
  created:
    - "modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/EntityClassResolver.java"
    - "modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/DynamicRepositoryException.java"
    - "modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/DynamicRepository.java"
  modified: []

key-decisions:
  - "Pre-instantiate new entities via EntityClassResolver + newInstance() before passing to converter, preventing AD_Table.javaClassName lookup"
  - "Do NOT call auditService.setAuditValues() in repository -- converter handles it internally (avoids duplicate)"
  - "Write methods use manual RestCallTransactionHandler (not @Transactional) to match generated repo pattern"
  - "Read methods use @Transactional for JPA session management"
  - "DefaultValuesHandler injected as Optional for safety -- may not have implementations"
  - "Only DIRECT_MAPPING fields supported for CriteriaBuilder filtering (other types lack entity properties)"
  - "convertExternalToInternalId deferred to Phase 4 controller (repository always receives internal IDs)"

patterns-established:
  - "Package convention: com.etendorx.das.repository for repository layer"
  - "EntityClassResolver as reusable @Component for any entity-class-from-tableId lookup"
  - "performSaveOrUpdateInternal as shared internal method for single save and batch save"
  - "ResponseStatusException for validation errors (BAD_REQUEST) and general errors (INTERNAL_SERVER_ERROR)"

# Metrics
duration: 3min
completed: 2026-02-06
---

# Phase 3 Plan 1: Generic Repository Layer Summary

**Full CRUD repository with metamodel-based entity resolution, CriteriaBuilder pagination/filtering, and BaseDTORepositoryDefault-compatible write flow including double merge, double externalIdService flush, and pre-instantiation bypass for AD_Table**

## Performance

- **Duration:** 3 min
- **Started:** 2026-02-06T19:02:32Z
- **Completed:** 2026-02-06T19:06:26Z
- **Tasks:** 3/3
- **Files created:** 3

## Accomplishments
- EntityClassResolver scans Hibernate metamodel at startup, builds ConcurrentHashMap indexes by TABLE_ID and @Table(name), resolves entity classes without any DB query
- DynamicRepository.findById resolves metadata -> resolves entity class -> EntityManager.find -> converter.convertToMap
- DynamicRepository.findAll uses CriteriaBuilder with dynamic predicates from DIRECT_MAPPING fields, Sort support, and PageImpl pagination
- Save/update/saveBatch replicate exact BaseDTORepositoryDefault order: upsert check -> pre-instantiate -> convert -> default values -> validate -> merge+flush -> externalId add+flush -> merge again -> postSync flush -> externalId flush -> return fresh read
- New entity pre-instantiation via EntityClassResolver ensures converter never triggers AD_Table.javaClassName JPQL lookup
- Audit values handled exclusively by converter (no duplicate call in repository)

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EntityClassResolver and DynamicRepositoryException** - `890aec5` (feat)
2. **Task 2: Create DynamicRepository read operations (findById, findAll)** - `7ba4e86` (feat)
3. **Task 3: Add DynamicRepository write operations (save, update, saveBatch)** - `7e4d394` (feat)

## Files Created/Modified
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/EntityClassResolver.java` - Metamodel-based entity class resolution by table ID and table name
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/DynamicRepositoryException.java` - Domain-specific runtime exception for repository errors
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/DynamicRepository.java` - Full CRUD + batch + pagination repository using EntityManager directly

## Decisions Made
- **Pre-instantiation pattern**: New entities are created via `entityClass.getDeclaredConstructor().newInstance()` using the class from EntityClassResolver, so the converter always receives a non-null entity and never triggers its internal AD_Table.javaClassName JPQL lookup. This enforces the locked decision "Hibernate metamodel for class resolution".
- **No duplicate audit**: DynamicDTOConverter.convertToEntity() already calls `auditServiceInterceptor.setAuditValues(rxObj)` at lines 192-194. Adding another call in the repository would cause double writes. Verified by absence of auditService.setAuditValues() in DynamicRepository.
- **Manual transaction management**: Write methods use `transactionHandler.begin()/commit()` instead of `@Transactional` because `RestCallTransactionHandler.commit()` uses `REQUIRES_NEW` and controls PostgreSQL trigger state.
- **Optional DefaultValuesHandler**: Injected as `Optional<DefaultValuesHandler>` since implementations may not exist. Called via `ifPresent()` in the save flow.
- **DIRECT_MAPPING-only filtering**: CriteriaBuilder predicates are only built for fields with `FieldMappingType.DIRECT_MAPPING` because other types (EM, JM, CV, JP) don't map directly to entity properties.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Repository layer complete with all CRUD operations for Phase 4 Generic REST Controller
- EntityClassResolver is a standalone @Component reusable by controller layer
- All 10 dependencies properly injected; no new libraries added (all already on classpath)
- convertExternalToInternalId intentionally deferred to Phase 4 controller (repository always receives internal IDs)
- Pre-existing compilation blocker in generated code still prevents test execution

---
*Phase: 03-generic-repository-layer*
*Completed: 2026-02-06*
