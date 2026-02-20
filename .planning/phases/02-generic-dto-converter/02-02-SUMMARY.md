---
phase: 02-generic-dto-converter
plan: 02
subsystem: converter
tags: [spring, entity-mapping, java-mapping, jsonpath, cycle-detection, external-id, audit-fields, strategy-pattern]

# Dependency graph
requires:
  - phase: 01-dynamic-metadata-service
    provides: FieldMetadata, EntityMetadata, ProjectionMetadata, DynamicMetadataService for field/entity resolution
  - phase: 02-01
    provides: FieldConversionStrategy interface, PropertyAccessorService, ConversionContext, ConversionException, three simple strategies (DM, CV, CM)
provides:
  - EntityMappingStrategy with recursive conversion, cycle detection, and ExternalId write resolution
  - JavaMappingStrategy with Spring bean qualifier-based read/write delegation
  - JsonPathStrategy with com.jayway.jsonpath extraction from JSON string properties
  - DynamicDTOConverter orchestrator with bidirectional Entity-Map conversion, mandatory validation, audit field integration, and entity instantiation
affects: [02-03, repository-layer, rest-controller]

# Tech tracking
tech-stack:
  added: []
  patterns: [Circular dependency resolution via @Lazy injection, Strategy dispatch via Map.of(enum -> strategy), ConcurrentHashMap caching for DB lookups]

key-files:
  created:
    - modules_core/com.etendorx.das/src/main/java/com/etendorx/das/converter/strategy/EntityMappingStrategy.java
    - modules_core/com.etendorx.das/src/main/java/com/etendorx/das/converter/strategy/JavaMappingStrategy.java
    - modules_core/com.etendorx.das/src/main/java/com/etendorx/das/converter/strategy/JsonPathStrategy.java
    - modules_core/com.etendorx.das/src/main/java/com/etendorx/das/converter/DynamicDTOConverter.java
  modified: []

key-decisions:
  - "@Lazy on DynamicDTOConverter constructor param in EntityMappingStrategy to break EM <-> Converter circular dependency"
  - "EntityMappingStrategy handles both Collection (one-to-many) and single entity (many-to-one) in readField"
  - "ExternalIdService.convertExternalToInternalId used for EM write path with fallback to raw ID"
  - "JavaMappingStrategy passes ctx.getFullDto() to DTOWriteMapping since its map(entity, dto) expects full DTO"
  - "JsonPathStrategy is read-only (write logs warning and no-ops), matching generated converter pattern"
  - "DynamicDTOConverter uses LinkedHashMap to preserve field order in output"
  - "Mandatory field validation excludes CV/CM types since constants are not DTO-sourced"
  - "AD_Table.javaClassName lookup cached in ConcurrentHashMap for entity instantiation"
  - "findEntityMetadataById is public on DynamicDTOConverter so EntityMappingStrategy can resolve related entity metadata"

patterns-established:
  - "Lazy injection: Use @Lazy on constructor params to break Spring circular dependencies between strategies and orchestrator"
  - "Strategy dispatch: Map<FieldMappingType, FieldConversionStrategy> with Map.of() for immutable enum-to-strategy mapping"
  - "Audit integration: Call auditServiceInterceptor.setAuditValues() after all field population in write path"
  - "Entity instantiation: JPQL lookup of AD_Table.javaClassName + Class.forName reflection with ConcurrentHashMap cache"

# Metrics
duration: 3min
completed: 2026-02-06
---

# Phase 02 Plan 02: Complex Strategies + DynamicDTOConverter Summary

**Three complex field strategies (EM with cycle detection, JM with Spring bean delegation, JP with JsonPath extraction) and DynamicDTOConverter orchestrator for bidirectional Entity-Map conversion with mandatory validation and audit integration**

## Performance

- **Duration:** 3 minutes
- **Started:** 2026-02-06T13:18:51Z
- **Completed:** 2026-02-06T13:22:00Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Built EntityMappingStrategy with recursive nested conversion, cycle detection via ConversionContext.isVisited(), support for both many-to-one and one-to-many Collection reads, and ExternalIdService-based write resolution
- Built JavaMappingStrategy delegating read to DTOReadMapping and write to DTOWriteMapping Spring beans resolved by qualifier, with full DTO context passing for write operations
- Built JsonPathStrategy extracting values from JSON string properties using com.jayway.jsonpath.JsonPath.read() with MappingUtils type coercion
- Built DynamicDTOConverter orchestrator wiring all 6 strategies, with convertToMap (Entity -> Map) and convertToEntity (Map -> Entity) including mandatory field validation, audit field integration, and entity instantiation via AD_Table JPQL lookup

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EntityMapping, JavaMapping, and JsonPath strategies** - `afdabf9` (feat)
2. **Task 2: Create DynamicDTOConverter orchestrator** - `dd79eb8` (feat)

## Files Created/Modified

**Complex strategies:**
- `EntityMappingStrategy.java` - EM strategy handling recursive entity conversion with cycle detection, both single-entity and Collection reads, ExternalId write resolution via JPQL
- `JavaMappingStrategy.java` - JM strategy delegating to DTOReadMapping/DTOWriteMapping Spring beans by qualifier, passing full DTO for write operations
- `JsonPathStrategy.java` - JP strategy extracting from JSON string properties using com.jayway.jsonpath, read-only (write no-ops)

**Orchestrator:**
- `DynamicDTOConverter.java` - Main converter with strategy dispatch, bidirectional conversion, mandatory validation, audit integration, entity instantiation, and findEntityMetadataById helper

## Decisions Made

1. **@Lazy circular dependency resolution** - EntityMappingStrategy needs DynamicDTOConverter for recursive convertToMap calls, creating a circular dependency. Resolved with @Lazy on the converter constructor parameter, which is the standard Spring approach.

2. **Collection handling in EntityMappingStrategy** - EM readField checks if the related value is a Collection and iterates elements individually with per-element cycle detection, producing List<Map<String, Object>>.

3. **Full DTO passing for JM write** - DTOWriteMapping.map(entity, dto) expects the complete DTO object, not a single field value. JavaMappingStrategy retrieves the full DTO from ConversionContext.getFullDto().

4. **JsonPath is read-only** - Generated converters never write to JP fields (they extract from existing JSON columns). JsonPathStrategy logs a warning and no-ops on write.

5. **LinkedHashMap for field order** - convertToMap returns LinkedHashMap to preserve field ordering from metadata (sorted by line number), ensuring consistent JSON output.

6. **Mandatory validation excludes constants** - CV and CM fields get their values from the database, not from DTO input, so they are excluded from mandatory field validation on write.

7. **Entity instantiation with caching** - AD_Table javaClassName lookup via JPQL is cached in ConcurrentHashMap to avoid repeated DB queries for the same table.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all strategies and orchestrator implemented smoothly using existing interfaces and infrastructure.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for Plan 03 (Unit Tests):**
- All 6 strategies (DM, CV, CM, EM, JM, JP) are implemented and wired
- DynamicDTOConverter is complete with bidirectional conversion
- Foundation from Plan 01 (interface, property accessor, context, exception) is stable
- Code follows established patterns (constructor injection, @Component, Slf4j logging)

**Remaining work in Phase 02:**
- Plan 03: Unit tests for all 6 strategies and DynamicDTOConverter

**No blockers.** All converter components are in place and ready for testing.

---
*Phase: 02-generic-dto-converter*
*Completed: 2026-02-06*
