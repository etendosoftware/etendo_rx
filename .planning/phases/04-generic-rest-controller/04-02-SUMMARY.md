---
phase: 04-generic-rest-controller
plan: 02
subsystem: api
tags: [rest-controller, spring-mvc, jsonpath, crud, pagination, batch]

# Dependency graph
requires:
  - phase: 04-generic-rest-controller/04-01
    provides: ExternalIdTranslationService and DynamicEndpointRegistry for controller support
  - phase: 03-generic-repository-layer
    provides: DynamicRepository with CRUD, batch, and pagination operations
  - phase: 02-generic-dto-converter
    provides: DynamicDTOConverter for entity/map conversion
  - phase: 01-dynamic-metadata-service
    provides: EntityMetadata records and DynamicMetadataService for runtime metadata
provides:
  - DynamicRestController with GET/POST/PUT CRUD endpoints
  - json_path support for JSON body extraction (Jayway JsonPath)
  - Batch entity creation via JSONArray detection
  - External ID translation on all write operations
  - Pagination, sorting, and filtering on list endpoint
affects: [04-generic-rest-controller/04-03, 05-coexistence-migration-support]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Generic REST controller with @RequestMapping(\"/{projectionName}/{entityName}\")"
    - "resolveEntityMetadata helper for validation + entity lookup"
    - "Jayway JsonPath for raw JSON parsing with json_path parameter"
    - "JSONArray instanceof check for batch vs single entity detection"

key-files:
  created:
    - modules_core/com.etendorx.das/src/main/java/com/etendorx/das/controller/DynamicRestController.java
  modified: []

key-decisions:
  - "POST uses Jayway JsonPath for JSON parsing (exact BindedRestController pattern)"
  - "Batch creation detected via JSONArray instanceof, not explicit parameter"
  - "PUT returns HttpStatus.CREATED (201) to match BindedRestController.put behavior"
  - "ExternalIdTranslationService called before repository delegation on all writes"
  - "Pagination params (page, size, sort) stripped from allParams for filter map"

patterns-established:
  - "resolveEntityMetadata pattern: resolve + validate restEndPoint in one helper"
  - "Error cascading: JsonProcessingException -> 400, ResponseStatusException -> rethrow, Exception -> 400"

# Metrics
duration: 2min
completed: 2026-02-06
---

# Phase 4 Plan 02: DynamicRestController Summary

**Single @RestController with GET/POST/PUT CRUD endpoints replacing all per-entity generated controllers, using Jayway JsonPath for json_path parsing and JSONArray batch support**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-06T22:52:29Z
- **Completed:** 2026-02-06T22:54:53Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments
- Created DynamicRestController as single generic REST controller for all projection entities
- GET list endpoint with pagination (@PageableDefault size=20), sorting, and DIRECT_MAPPING filter support
- GET by ID endpoint with EntityNotFoundException -> 404 mapping
- POST endpoint with Jayway JsonPath parsing, JSONArray batch detection, and json_path parameter
- PUT endpoint with path ID override and ObjectMapper parsing
- External ID translation applied to all write operations before repository delegation

## Task Commits

Each task was committed atomically:

1. **Task 1: Create DynamicRestController with GET endpoints** - `043278c` (feat)
2. **Task 2: Add POST and PUT endpoints with json_path and batch support** - `c871ad7` (feat)

**Plan metadata:** (pending)

## Files Created/Modified
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/controller/DynamicRestController.java` - Single generic REST controller handling all CRUD operations for dynamically-served projections

## Decisions Made
- **POST uses Jayway JsonPath parsing** - Exact replication of BindedRestController.parseJson() pattern with Configuration.defaultConfiguration().addOptions()
- **Batch detected via JSONArray instanceof** - Matches BindedRestController.handleRawData() which checks `rawData instanceof JSONArray`
- **PUT returns 201 (CREATED)** - Matches BindedRestController.put() which returns `new ResponseEntity<>(result, HttpStatus.CREATED)`
- **ExternalIdTranslationService called on all writes** - Ensures repository always receives internal IDs (deferred from Phase 3)
- **Pagination params stripped from filters** - page, size, sort removed from allParams to avoid spurious CriteriaBuilder predicates

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- DynamicRestController fully implements GET, POST, PUT CRUD operations
- Ready for Plan 03 (unit tests) to verify all endpoint behavior
- DELETE endpoint not in scope (BindedRestController also lacks DELETE)
- Pre-existing compilation issues in generated code still block runtime testing

---
*Phase: 04-generic-rest-controller*
*Completed: 2026-02-06*
