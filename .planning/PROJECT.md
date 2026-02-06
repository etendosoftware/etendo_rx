# PROJECT.md

## Project Name
Dynamic DAS Mapping Layer (Option 5)

## Vision
Eliminate the code generation + compilation cycle for DAS entity mappings by making DTOs, converters, controllers, and field mappings fully dynamic at runtime. JPA entities remain generated (they change rarely), but everything above them becomes metadata-driven, reading `etrx_*` configuration tables directly.

## Problem Statement
Currently, any change to entity projections, field mappings, or connector configurations requires:
1. Modifying `etrx_*` configuration tables in the database
2. Running `generate.entities` Gradle task (FreeMarker templates -> Java source)
3. Compiling the generated code
4. Restarting the DAS service

This creates a slow feedback loop, makes it impossible to add/modify mappings at runtime, and forces a full redeployment for configuration-only changes. The generated code (DTOs, converters, controllers, retrievers) follows predictable patterns that can be interpreted at runtime.

## Solution: Pragmatic Hybrid Approach (Option 5)

**Keep generated (change rarely):**
- JPA Entity classes (Hibernate mappings to database tables)
- JPA Repositories (Spring Data interfaces)
- Base entity model (`modules_gen/com.etendorx.entities/src/main/entities/`)

**Make dynamic at runtime:**
- DTOs (Read/Write) -> `Map<String, Object>` driven by `etrx_projection_entity` + `etrx_entity_field`
- Converters (Entity <-> DTO) -> Generic converter reading field metadata from DB
- REST Controllers -> Single generic controller dispatching based on projection/entity name
- JsonPath converters -> Dynamic field extraction using metadata-driven jsonpath expressions
- Field mappings -> Already partially dynamic via `OBCONFieldMapping`, extend to all mappings
- External ID resolution -> Already dynamic, no changes needed

## Key Technical Decisions

1. **Generic DTO representation**: Use `Map<String, Object>` instead of typed DTO classes
2. **Metadata-driven conversion**: Read `etrx_entity_field` at runtime to know which entity properties map to which DTO fields
3. **Dynamic REST endpoints**: Register/unregister REST routes based on `etrx_projection` table contents
4. **Caching layer**: Cache metadata reads (projection definitions, field mappings) with invalidation on config change
5. **Backwards compatibility**: Existing generated code continues to work; dynamic layer is additive, not replacement initially

## Architecture

```
HTTP Request
    |
    v
[Generic REST Controller]  -- reads projection metadata from cache/DB
    |
    v
[Dynamic DTO Converter]    -- converts Entity <-> Map<String,Object> using field metadata
    |
    v
[JPA Repository]           -- (still generated, stays as-is)
    |
    v
[JPA Entity]               -- (still generated, stays as-is)
    |
    v
[Database]
```

## Tech Stack Context
- Java 17, Spring Boot 3.1.4, Spring Cloud 2022.0.4
- Spring Data JPA + Hibernate
- PostgreSQL (primary), Oracle (secondary)
- Kafka for async processing
- JWT authentication via Edge gateway
- Existing codebase: ~200+ source files across 15+ modules

## Constraints
- Must not break existing generated mapping flow (coexistence during migration)
- Must maintain JWT authentication and authorization model
- Must maintain ExternalId resolution for connector integrations
- Must support existing connector field mapping patterns (OBCONFieldMapping)
- Performance: Dynamic resolution must not significantly degrade REST API response times
- Must work with existing Spring Data REST projections until fully migrated

## Success Criteria
- New projections/entities can be exposed via REST without code generation or restart
- Field mapping changes take effect without recompilation
- Existing generated code continues to function during migration period
- API response format remains compatible with current consumers
- Metadata caching ensures performance parity with generated code

## Milestone 1: Dynamic DAS Core
Build the foundation: generic controller, dynamic DTO conversion, metadata caching, and integration with existing JPA layer.

## Team Context
- Etendo RX development team
- Existing familiarity with Spring Boot, JPA, code generation pipeline
- Codebase already mapped in `.planning/codebase/`

## Codebase State
- Brownfield: Large existing codebase with established patterns
- Generated code in `modules_gen/`
- Core libraries in `libs/`
- Service modules in `modules_core/`
- Integration modules in `modules/`
- Full codebase analysis available in `.planning/codebase/*.md`
