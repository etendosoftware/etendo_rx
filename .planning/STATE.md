# STATE.md

## Current State
- **Milestone:** 1 - Dynamic DAS Core
- **Phase:** 3 - Generic Repository Layer (COMPLETE + VERIFIED)
- **Plan:** 02 of 02 (completed)
- **Last activity:** 2026-02-06 - Phase 3 complete, 11/11 must-haves verified
- **Next action:** Phase 4 (Generic REST Controller & Endpoint Registration)
- **Verification:** Phase 3 PASSED (11/11)

**Progress:** ████████░░░░░░░░ 8/12 plans complete (67%)

## Phase Status
| Phase | Name | Status |
|-------|------|--------|
| 1 | Dynamic Metadata Service | COMPLETE + VERIFIED (14/14 must-haves, tests blocked from execution) |
| 2 | Generic DTO Converter | All 3 plans complete, awaiting phase verification |
| 3 | Generic Repository Layer | COMPLETE + VERIFIED (11/11 must-haves, tests blocked from execution) |
| 4 | Generic REST Controller & Endpoint Registration | pending |
| 5 | Coexistence & Migration Support | pending |

## Key Decisions

| Decision | Phase | Rationale |
|----------|-------|-----------|
| JPA entities remain generated (not part of this project) | Initial | Existing code generation continues |
| DTOs represented as `Map<String, Object>` at runtime | Initial | Generic runtime representation |
| Single generic controller pattern (not one controller per entity) | Initial | Simplify endpoint management |
| Metadata cached in memory with manual invalidation initially | Initial | Performance optimization |
| Coexistence: dynamic endpoints are additive, generated ones unchanged | Initial | Non-breaking migration path |
| Use Java records for metadata models | 01-01 | Immutability ensures thread-safe caching |
| Separate models from service interface | 01-01 | Clear separation of concerns, prevents context exhaustion |
| Include findEntity helper in ProjectionMetadata | 01-01 | Common lookup pattern convenience |
| Caffeine cache with 500 max entries and 24-hour expiration | 01-02 | Balances memory usage with typical projection count |
| Preload all projections at startup | 01-02 | Avoids cold start latency on first requests |
| Sort fields by line number during conversion | 01-02 | Maintains consistent display order |
| Fallback to DB for getFields() on cache miss | 01-02 | Ensures method robustness for edge cases |
| Default to DIRECT_MAPPING for unknown field mapping types | 01-02 | Prevents application crash from data inconsistencies |
| Use real Caffeine cache in tests rather than mocking | 01-03 | Accurate cache behavior verification |
| Use Apache Commons BeanUtils for nested property access | 02-01 | Handles dot notation and null intermediate properties gracefully |
| PropertyAccessorService returns null instead of throwing | 02-01 | Matches generated converter behavior for missing properties |
| ConversionContext tracks visited entities by class+id | 02-01 | Prevents infinite recursion in circular entity relationships |
| DirectMappingStrategy chains getNestedProperty -> handleBaseObject | 02-01 | Replicates generated converter type coercion behavior |
| Constant strategies (CV, CM) are read-only | 02-01 | Generated converters never write constant fields |
| @Lazy on DynamicDTOConverter in EntityMappingStrategy | 02-02 | Breaks circular dependency EM <-> Converter |
| EM handles both Collection and single entity | 02-02 | Supports one-to-many and many-to-one relations |
| JM write passes full DTO via ConversionContext | 02-02 | DTOWriteMapping.map(entity, dto) expects complete DTO |
| JP strategy is read-only | 02-02 | Generated converters never write to JsonPath fields |
| LinkedHashMap for field order preservation | 02-02 | Consistent JSON output matching metadata line ordering |
| Mandatory validation excludes CV/CM | 02-02 | Constants sourced from DB, not DTO input |
| AD_Table.javaClassName cached in ConcurrentHashMap | 02-02 | Avoids repeated JPQL lookups for entity instantiation |
| Manual constructor injection in tests for @Lazy params | 02-03 | @InjectMocks incompatible with @Lazy constructor params |
| ArgumentCaptor for ConversionContext fullDto verification | 02-03 | Captures internally-created context for deep assertion |
| Pre-instantiate new entities via EntityClassResolver + newInstance() | 03-01 | Prevents converter from triggering AD_Table.javaClassName JPQL lookup |
| Do NOT call auditService.setAuditValues() in repository | 03-01 | Converter already calls it internally, avoids duplicate audit writes |
| Write methods use manual transactionHandler (not @Transactional) | 03-01 | RestCallTransactionHandler.commit() uses REQUIRES_NEW for trigger control |
| Only DIRECT_MAPPING fields for CriteriaBuilder filtering | 03-01 | Other mapping types (EM, JM, CV, JP) lack direct entity properties |
| DefaultValuesHandler injected as Optional | 03-01 | Safety for cases where no implementation exists |
| convertExternalToInternalId deferred to Phase 4 controller | 03-01 | Repository always receives internal IDs; translation is controller concern |
| Inner test entity classes with @Table for EntityClassResolver tests | 03-02 | Avoids dependency on generated entities with compilation issues |
| LENIENT strictness for DynamicRepositoryTest | 03-02 | Complex save stubs shared across tests; not all used by every test |
| CriteriaBuilder helper method for findAll tests | 03-02 | Reduces verbose mock chain duplication |

## Blockers & Concerns

### Critical Blockers
1. **Project-wide compilation issues (Pre-existing)** - Discovered during Phase 01 Plan 03
   - Generated entity metadata files have incorrect constructor calls
   - Generated DTO converter files missing abstract method implementations
   - Prevents test execution and new development
   - **Impact:** Cannot verify tests pass, blocks Phase 02 development
   - **Required Action:** Fix FreeMarker code generation templates OR use pre-compiled JARs
   - **Tracked In:** 01-03-SUMMARY.md

## Session Continuity

- **Last session:** 2026-02-06T19:12:45Z
- **Stopped at:** Completed 03-02-PLAN.md
- **Resume file:** None

## Context Files
- `.planning/PROJECT.md` - Project definition and vision
- `.planning/REQUIREMENTS.md` - Functional and non-functional requirements
- `.planning/ROADMAP.md` - Phase breakdown and dependencies
- `.planning/codebase/` - 7 codebase analysis documents
- `CONNECTORS.md` - DAS data flow documentation
- `.planning/phases/01-dynamic-metadata-service/01-01-SUMMARY.md` - Metadata models and service interface completed
- `.planning/phases/01-dynamic-metadata-service/01-02-SUMMARY.md` - Cache configuration and service implementation completed
- `.planning/phases/01-dynamic-metadata-service/01-03-SUMMARY.md` - Unit tests completed (blocked from execution)
- `.planning/phases/01-dynamic-metadata-service/01-VERIFICATION.md` - Phase 1 verification report (14/14 passed)
- `.planning/phases/02-generic-dto-converter/02-01-SUMMARY.md` - Converter foundation with strategy pattern and three simple strategies (DM, CV, CM)
- `.planning/phases/02-generic-dto-converter/02-02-SUMMARY.md` - Complex strategies (EM, JM, JP) and DynamicDTOConverter orchestrator
- `.planning/phases/02-generic-dto-converter/02-03-SUMMARY.md` - 27 unit tests for DM strategy, EM strategy, and converter orchestrator
- `.planning/phases/03-generic-repository-layer/03-01-SUMMARY.md` - EntityClassResolver, DynamicRepository with full CRUD + batch + pagination
- `.planning/phases/03-generic-repository-layer/03-02-SUMMARY.md` - 27 unit tests for EntityClassResolver (8) and DynamicRepository (19)
