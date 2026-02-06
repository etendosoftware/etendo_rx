# Phase 4: Generic REST Controller & Endpoint Registration - Research

**Researched:** 2026-02-06
**Domain:** Spring Boot REST controller with dynamic URL routing, pagination, batch processing, JSON-path extraction, external ID translation, and OpenAPI documentation
**Confidence:** HIGH

## Summary

This phase creates a single `DynamicRestController` that dynamically serves all projections registered in the metadata service, replacing per-entity generated REST controllers. The controller must match the **exact URL pattern** used by generated controllers: `/{mappingPrefix}/{externalName}` where `mappingPrefix` is the **lowercased projection name** (from `projection.getName().toUpperCase()` lowercased via FreeMarker `?lower_case`).

The existing `BindedRestController` (in `libs/com.etendorx.das_core`) provides the canonical REST patterns that the dynamic controller must replicate: `findAll` with `@PageableDefault(size=20)`, `get/{id}` returning `ResponseEntity`, `post` with `json_path` parameter and `JSONArray` batch support, and `put/{id}`. The generated controller template (`baseRestController.ftl`) shows how each generated controller extends `BindedRestController` and adds a `@RequestMapping("/${mappingPrefix?lower_case}/${entity.externalName}")`.

Authentication works via `FilterContext` (a `OncePerRequestFilter` in `libs/com.etendorx.utils.auth`) which intercepts every request, extracts JWT from `X-TOKEN` header or `Authorization: Bearer` header, and populates `AppContext.currentUser`. The DAS module has **no separate SecurityConfig** -- it relies entirely on `FilterContext` from the auth library. Dynamic endpoints will automatically be authenticated as long as their URLs are not in `AllowedURIS` exclusion lists.

**Primary recommendation:** Create a single `@RestController` with `@RequestMapping("/{projectionName}/{entityName}")` path variables that resolves projection+entity metadata at request time, delegates to `DynamicRepository` for all CRUD operations, and handles `convertExternalToInternalId` for incoming entity reference IDs in POST/PUT bodies.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot Web | 3.1.4 | @RestController, @RequestMapping, ResponseEntity | Already in build.gradle |
| Spring Data Commons | (Boot 3.1.4) | Page, Pageable, @PageableDefault, Sort | Already on classpath |
| Jayway JsonPath | 2.8.0 | json_path parameter parsing for nested JSON extraction | Already in das_core dependency |
| Jackson Databind | 2.17.1 | ObjectMapper for JSON serialization/deserialization | Already on classpath |
| SpringDoc OpenAPI | 2.2.0 | Swagger/OpenAPI documentation via annotations | Already in DAS build.gradle |
| io.swagger.v3 annotations | 2.2.16 | @Operation, @SecurityRequirement annotations | Already in das_core |
| net.minidev json-smart | 2.5.1 | JSONArray for batch POST parsing | Already in das_core |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Jakarta Validation | 3.0 | @Valid, Validator for DTO validation | Already on classpath |
| Spring Boot Actuator | 3.1.4 | Health checks, metrics | Already in build.gradle |
| Log4j2 / SLF4J | (Boot managed) | Logging of dynamic endpoint access | Existing logging framework |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Single controller with path variables | RequestMappingHandlerMapping programmatic registration | Path variables are simpler and match existing pattern; programmatic registration adds complexity with no benefit |
| @PageableDefault annotation | Manual Pageable construction | Annotation is consistent with existing controllers |
| Jackson ObjectMapper in controller | Spring's built-in @RequestBody Map | Need raw String body for json_path support (same as BindedRestController) |

**Installation:**
```bash
# No new dependencies needed. All required libraries are already in build.gradle.
```

## Architecture Patterns

### Recommended Project Structure
```
com.etendorx.das.controller/
  DynamicRestController.java          # Single generic REST controller
  DynamicEndpointRegistry.java        # Startup logging + URL validation
  ExternalIdTranslationService.java   # convertExternalToInternalId orchestration for controller
```

### Pattern 1: URL Pattern Matching
**What:** The generated controllers use URL pattern `/{projectionName_lowercase}/{entityExternalName}`. The `mappingPrefix` is `projection.getName().toUpperCase()` in code generation, then lowercased in the FreeMarker template via `${mappingPrefix?lower_case}`. So the URL prefix is the **lowercase projection name**. The entity external name comes from `ETRXProjectionEntity.externalName`.
**When to use:** All dynamic endpoint routing.
**Evidence:**
```java
// Source: baseRestController.ftl line 42
@RequestMapping("/${mappingPrefix?lower_case}/${entity.externalName}")

// Source: MappingGenerationBase.java line 58
final String mappingPrefix = etrxProjectionEntity.getProjection().getName().toUpperCase();

// Therefore: if projection name = "OBMAP", the URL is /obmap/{externalName}
// The test RestCallTest uses: get("/" + model + "/" + id + "?projection=default")
// This suggests a DIFFERENT routing pattern for tests (/{entityExternalName}/{id}?projection=...)
// But the actual generated controllers use: /{projection_lowercase}/{externalName}
```

**CRITICAL INSIGHT:** The RestCallTest URL pattern `/{entityName}/{id}?projection=default` appears to be a **test-only** routing pattern. The production-generated controllers use `/{mappingPrefix?lower_case}/${entity.externalName}` with NO query parameter. The dynamic controller should use the generated controller's URL pattern.

**Dynamic controller URL:** `/{projectionName}/{entityName}` where both are path variables.
```java
// Source: Verified from baseRestController.ftl
@RestController
@RequestMapping("/{projectionName}/{entityName}")
public class DynamicRestController {
    // projectionName = lowercase projection name (e.g., "obmap")
    // entityName = entity external name (e.g., "Product")
}
```

### Pattern 2: Batch POST with json_path
**What:** The BindedRestController.post() method accepts raw JSON as String, applies json_path using Jayway JsonPath to extract a subset, then handles JSONArray (batch) or Map (single) cases. The json_path parameter defaults to "$" (whole document).
**When to use:** POST endpoint for entity creation.
**Evidence:**
```java
// Source: BindedRestController.java lines 137-157
@PostMapping
public ResponseEntity<Object> post(@RequestBody String rawEntity,
    @RequestParam(required = false, name = "json_path") String jsonPath) {
    jsonPath = (StringUtils.hasText(jsonPath)) ? jsonPath : "$";
    Object rawData = parseJson(rawEntity, jsonPath);
    return new ResponseEntity<>(handleRawData(rawData, rawEntity), HttpStatus.CREATED);
}

// parseJson uses JsonPath.using(conf).parse(rawEntity).read(jsonPath, Object.class)
// handleRawData: if rawData instanceof JSONArray -> batch, if Map -> single, else fallback
```

### Pattern 3: External ID Translation at Controller Level
**What:** When an incoming POST/PUT body contains entity reference fields (e.g., organization ID, business partner ID), those IDs may be external system IDs that need conversion to internal IDs via `ExternalIdService.convertExternalToInternalId()`. The generated code handles this in `JsonPathEntityRetrieverBase.get()` which calls `convertExternalToInternalId` per field. For the dynamic controller, this translation should happen BEFORE calling DynamicRepository.
**When to use:** POST and PUT endpoints, for all entity reference fields in the incoming DTO.
**Evidence:**
```java
// Source: JsonPathEntityRetrieverBase.java lines 108-112
for (String key : keys) {
    String idReceived = valueIterator.next();
    final String value = getExternalIdService().convertExternalToInternalId(getTableId(), idReceived);
    specs.add((root, query, builder) -> builder.equal(root.get(key), value));
}
// This translates IDs for entity RETRIEVAL (identifiesUnivocally fields)
// For the dynamic controller, we need to translate reference IDs in the DTO body
```

### Pattern 4: Response Format
**What:** The generated controllers return:
- GET list: `Page<E>` (Spring Data Page, serialized by Jackson to standard Spring pagination JSON)
- GET by ID: `ResponseEntity<E>` with HttpStatus.OK
- POST: `ResponseEntity<Object>` with HttpStatus.CREATED (single entity or list for batch)
- PUT: `ResponseEntity<E>` with HttpStatus.CREATED
- 404: `ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found")`
- 400: `ResponseStatusException(HttpStatus.BAD_REQUEST, message)`
**When to use:** All controller responses must match this format.
**Evidence:**
```java
// Source: BindedRestController.java
// GET list returns Page<E> which Jackson serializes as:
// { "content": [...], "pageable": {...}, "totalPages": N, "totalElements": N, ... }

// For dynamic controller, Page<Map<String,Object>> will be serialized similarly
// since Map<String,Object> serializes to JSON naturally.
```

### Pattern 5: Authentication Integration
**What:** Authentication is handled by `FilterContext` (a `OncePerRequestFilter` component in `com.etendorx.utils.auth.key.context`). It intercepts ALL requests, extracts JWT from `X-TOKEN` header or `Authorization: Bearer` header, validates the token, and sets `AppContext.currentUser`. No separate SecurityConfig exists in the DAS module. The dynamic controller needs NO additional auth configuration -- it automatically gets authenticated as long as URLs are not in `AllowedURIS` exclusion lists.
**When to use:** No action needed for auth in the controller; it works automatically.
**Evidence:**
```java
// Source: FilterContext.java lines 60-87
// Token extracted from X-TOKEN header or Authorization: Bearer
// AppContext.setCurrentUser(userContext) called after validation
// ExternalIdService uses AppContext.getCurrentUser().getExternalSystemId()

// Source: GlobalAllowedURIS.java
// Only /actuator/, /v3/api-docs, /api-docs, /swagger-ui, .png, .ico are allowed without auth
// Dynamic controller URLs /{projectionName}/{entityName} require auth by default
```

### Anti-Patterns to Avoid
- **Don't extend BindedRestController:** It is generic-typed with `<E extends BaseDTOModel, F extends BaseDTOModel>`. Our dynamic controller uses `Map<String,Object>` which does NOT implement `BaseDTOModel`. Build a standalone controller.
- **Don't use @Transactional on the controller:** The generated template uses `@Transactional` on the controller but `DynamicRepository` already handles its own transactions. Only read operations in the controller should be `@Transactional(readOnly=true)` if needed; write operations delegate to repository which uses `RestCallTransactionHandler`.
- **Don't register endpoints programmatically:** Using `RequestMappingHandlerMapping` at startup to register individual endpoints is unnecessarily complex. A single controller with path variables achieves the same result.
- **Don't build a custom JSON response wrapper:** Spring's Page serialization and ResponseEntity provide the exact format the generated controllers produce.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON path extraction | Custom JSON parsing | Jayway JsonPath `DocumentContext.read()` | Already used by BindedRestController; handles edge cases |
| Pagination response format | Custom page wrapper | Spring's `Page<Map<String,Object>>` via `PageImpl` | Jackson serializes it identically to generated controllers |
| Batch detection | Custom array check | `rawData instanceof JSONArray` | BindedRestController pattern; handles net.minidev JSONArray correctly |
| External ID translation | Custom ID lookup | `ExternalIdService.convertExternalToInternalId()` | Already exists, handles fallback (returns original if not found) |
| Request validation (empty body) | Custom validation | `StringUtils.hasText()` check | Same pattern as BindedRestController |
| Error responses | Custom error objects | `ResponseStatusException` with HTTP status codes | Standard Spring approach, consistent with BindedRestController |
| OpenAPI docs | Custom swagger config | SpringDoc annotations `@Operation`, `@SecurityRequirement` | Already configured with `springdoc-openapi-starter-webmvc-ui:2.2.0` |

**Key insight:** The BindedRestController contains 246 lines of battle-tested REST logic. The dynamic controller should replicate its exact behavior patterns but operate on `Map<String,Object>` instead of typed DTOs.

## Common Pitfalls

### Pitfall 1: URL Pattern Mismatch
**What goes wrong:** Dynamic endpoints don't match the URL patterns generated controllers create, breaking clients.
**Why it happens:** Confusion between the test URL pattern (`/{entityName}/{id}?projection=`) and the generated controller pattern (`/{projectionPrefix_lowercase}/{externalName}`).
**How to avoid:** The URL MUST be `/{projectionName_lowercase}/{entityExternalName}`. The `projectionName` is `projection.getName()` lowercased. The `entityExternalName` is `ETRXProjectionEntity.externalName`. Both are in metadata.
**Warning signs:** Existing clients getting 404s on URLs that worked with generated controllers.

### Pitfall 2: Response Format Differences
**What goes wrong:** Dynamic endpoints return JSON with different structure than generated DTOs.
**Why it happens:** `Map<String,Object>` serializes field names as-is, but generated DTOs use Java getter naming conventions. If field names in metadata don't match generated DTO field names exactly, the JSON differs.
**How to avoid:** Field names in `FieldMetadata.name()` already match the generated DTO field names (they come from the same etrx_entity_field table). Use `LinkedHashMap` to preserve field order (already done in DynamicDTOConverter).
**Warning signs:** Integration tests comparing dynamic vs generated JSON responses show differences.

### Pitfall 3: Missing Transaction Management for Read Operations
**What goes wrong:** Lazy-loaded entity relationships cause `LazyInitializationException` during conversion.
**Why it happens:** `DynamicRepository.findById()` and `findAll()` are `@Transactional` but if the controller initiates conversion outside that transaction scope, lazy proxies are detached.
**How to avoid:** Ensure the entire read + convert flow happens within a single transaction boundary. `DynamicRepository` already handles this -- `findById` converts within its `@Transactional` method. Don't add a second conversion step in the controller.
**Warning signs:** `LazyInitializationException` on entity relationship fields.

### Pitfall 4: convertExternalToInternalId Not Called
**What goes wrong:** Incoming POST/PUT with external system reference IDs are stored as-is, creating orphaned references.
**Why it happens:** The generated flow calls `convertExternalToInternalId` in `JsonPathEntityRetrieverBase.get()` (for lookup by `identifiesUnivocally` fields). The dynamic flow needs to call it for all reference fields in the incoming DTO.
**How to avoid:** Before calling `DynamicRepository.save()`, iterate the incoming DTO map, identify fields that are entity references (ENTITY_MAPPING type with `identifiesUnivocally=true`), and translate their IDs using `ExternalIdService.convertExternalToInternalId()`. The `tableId` for translation comes from the related entity's table.
**Warning signs:** External systems sending their IDs but entities being created with those external IDs stored directly.

### Pitfall 5: json_path Defaulting
**What goes wrong:** POST without `json_path` parameter fails or doesn't extract data correctly.
**Why it happens:** Forgetting to default `json_path` to `"$"` when not provided.
**How to avoid:** Exactly replicate BindedRestController: `jsonPath = (StringUtils.hasText(jsonPath)) ? jsonPath : "$"`.
**Warning signs:** POST requests without json_path parameter returning 400.

### Pitfall 6: Metadata Resolution Failure Not Handled Gracefully
**What goes wrong:** Request to URL with non-existent projection or entity returns 500 instead of 404.
**Why it happens:** `metadataService.getProjection()` returns `Optional.empty()` but controller doesn't check.
**How to avoid:** Check metadata resolution results and return 404 with descriptive message when projection or entity not found.
**Warning signs:** HTTP 500 errors when requesting non-existent projections.

### Pitfall 7: PUT Response Status
**What goes wrong:** PUT returns 200 but generated controller returns 201.
**Why it happens:** Different conventions for PUT responses.
**How to avoid:** Match exactly: BindedRestController.put() returns `new ResponseEntity<>(result, HttpStatus.CREATED)` -- HTTP 201.
**Warning signs:** Integration test status code mismatch.

### Pitfall 8: OpenAPI GroupedOpenApi Conflict
**What goes wrong:** Dynamic endpoints don't appear in Swagger UI, or conflict with generated endpoint documentation.
**Why it happens:** The generated `OpenApiConfig.java` creates GroupedOpenApi beans per projection. Dynamic endpoints need their own grouping.
**How to avoid:** Create a separate GroupedOpenApi bean for dynamic endpoints using a "dynamic" group, or add a catch-all group that matches dynamic endpoint paths.
**Warning signs:** Swagger UI doesn't show dynamic endpoints, or shows duplicate entries.

## Code Examples

Verified patterns from existing codebase:

### Dynamic Controller Structure
```java
// Based on: BindedRestController pattern + baseRestController.ftl
@RestController
@RequestMapping("/{projectionName}/{entityName}")
@Slf4j
public class DynamicRestController {

    private final DynamicRepository repository;
    private final DynamicMetadataService metadataService;
    private final ExternalIdService externalIdService;

    // GET list - matches BindedRestController.findAll()
    @GetMapping
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public Page<Map<String, Object>> findAll(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @PageableDefault(size = 20) Pageable pageable) {
        // Resolve metadata, delegate to repository
    }

    // GET by ID - matches BindedRestController.get()
    @GetMapping("/{id}")
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public ResponseEntity<Map<String, Object>> findById(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @PathVariable String id) {
        // Resolve metadata, delegate to repository, 404 if not found
    }

    // POST - matches BindedRestController.post()
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public ResponseEntity<Object> create(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @RequestBody String rawEntity,
            @RequestParam(required = false, name = "json_path") String jsonPath) {
        // Parse json_path, handle batch vs single, delegate to repository
    }

    // PUT - matches BindedRestController.put()
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(security = { @SecurityRequirement(name = "basicScheme") })
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String projectionName,
            @PathVariable String entityName,
            @PathVariable String id,
            @RequestBody String rawEntity) {
        // Resolve metadata, parse JSON to map, set ID, delegate to repository
    }
}
```

### Metadata Resolution Pattern
```java
// Resolve projection + entity from path variables
private EntityMetadata resolveEntityMetadata(String projectionName, String entityName) {
    ProjectionMetadata projection = metadataService.getProjection(projectionName.toUpperCase())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Projection not found: " + projectionName));

    return projection.findEntity(entityName)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Entity not found: " + entityName + " in projection: " + projectionName));
}
```

**IMPORTANT NOTE on projection name resolution:** The URL uses lowercased projection name (e.g., `/obmap/Product`). The metadata service stores projections by their original name (e.g., `"OBMAP"`). The controller must normalize: `metadataService.getProjection(projectionName.toUpperCase())`.

### JSON Path Extraction Pattern (from BindedRestController)
```java
// Source: BindedRestController.java lines 166-170
protected Object parseJson(String rawEntity, String jsonPath) {
    Configuration conf = Configuration.defaultConfiguration().addOptions();
    DocumentContext documentContext = JsonPath.using(conf).parse(rawEntity);
    return documentContext.read(jsonPath, Object.class);
}
```

### Batch Processing Pattern (from BindedRestController)
```java
// Source: BindedRestController.java lines 183-203
ObjectMapper objectMapper = new ObjectMapper();
if (rawData instanceof JSONArray) {
    List<Map<String, Object>> results = new ArrayList<>();
    for (Object rawDatum : ((JSONArray) rawData)) {
        if (rawDatum instanceof Map) {
            // Convert back to JSON string, then parse to Map
            String jsonObject = objectMapper.writeValueAsString(rawDatum);
            // Process single entity
        }
    }
    return results;
} else if (rawData instanceof Map) {
    // Single entity
} else {
    // Fallback: treat raw string as single entity
}
```

### External ID Translation Pattern
```java
// Source: ExternalIdServiceImpl.java lines 173-184
// Called PER reference field in incoming DTO before save
String internalId = externalIdService.convertExternalToInternalId(
    relatedEntityTableId,  // table ID of the referenced entity
    externalId             // the ID value from the incoming DTO
);
// Returns the internal ID if mapping exists, otherwise returns the original value
```

### Endpoint Startup Logging Pattern
```java
// Log all registered dynamic endpoints at startup
@EventListener(ApplicationReadyEvent.class)
public void logDynamicEndpoints() {
    for (String projectionName : metadataService.getAllProjectionNames()) {
        ProjectionMetadata projection = metadataService.getProjection(projectionName).orElse(null);
        if (projection != null) {
            for (EntityMetadata entity : projection.entities()) {
                if (entity.restEndPoint()) {
                    log.info("Dynamic endpoint registered: /{}/{}",
                        projectionName.toLowerCase(), entity.externalName());
                }
            }
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Generated per-entity controllers | Single dynamic controller with path variables | This phase | Eliminates code generation for controllers |
| Typed DTO classes (BaseDTOModel) | Map<String,Object> runtime DTOs | Phase 2 | No compilation needed for new entities |
| Per-entity JsonPathConverter | DynamicDTOConverter with strategy pattern | Phase 2 | Generic conversion for all entities |
| Per-entity DASRepository | DynamicRepository with EntityManager | Phase 3 | Generic CRUD for all entities |

**Deprecated/outdated:**
- None. This phase creates new functionality alongside existing generated code.
- The generated `BindedRestController` pattern remains the reference implementation.

## Open Questions

Things that couldn't be fully resolved:

1. **Projection Name Casing in URL**
   - What we know: Generated controller FTL uses `${mappingPrefix?lower_case}` which lowercases the projection name for the URL path.
   - What's unclear: Whether clients use exactly lowercase or mixed case when calling the API.
   - Recommendation: Accept case-insensitive projection names in the URL (uppercase before metadata lookup). Store/log in lowercase.

2. **restEndPoint Flag Usage**
   - What we know: `EntityMetadata.restEndPoint()` is a boolean that indicates whether the entity should be exposed as a REST endpoint. The generated controller template always generates a controller for each entity.
   - What's unclear: Whether the generated code actually checks `restEndPoint` before generating a controller, or always generates.
   - Recommendation: The dynamic controller SHOULD check `entity.restEndPoint() == true` and return 404 for entities where it is false.

3. **convertExternalToInternalId Scope**
   - What we know: In generated code, `JsonPathEntityRetrieverBase.get()` calls `convertExternalToInternalId` for `identifiesUnivocally` fields during entity RETRIEVAL. The `EntityMappingStrategy` already handles reference entity lookup in the converter.
   - What's unclear: Whether the dynamic controller also needs to translate the top-level "id" field in POST bodies, or only reference fields.
   - Recommendation: Translate the "id" field if present in POST (for upsert matching), and translate all fields that represent entity references. The `EntityMappingStrategy.writeField()` already resolves references by ID during conversion, but those IDs may be external. The controller should call `convertExternalToInternalId` on the "id" field and on all ENTITY_MAPPING fields before passing to the repository.

4. **OpenAPI Documentation for Dynamic Endpoints**
   - What we know: The generated `OpenApiConfig.java` creates `GroupedOpenApi` beans per projection with path matching `/${projectionName}/**`.
   - What's unclear: Whether SpringDoc automatically documents path-variable-based controllers or if custom configuration is needed.
   - Recommendation: Add `@Operation` annotations to the controller methods. Create a dynamic `GroupedOpenApi` bean (or rely on default ungrouped docs). Since the dynamic controller uses path variables, SpringDoc will document it as a single endpoint with parameters.

## Sources

### Primary (HIGH confidence)
- `libs/com.etendorx.das_core/src/main/java/com/etendorx/entities/mapper/lib/BindedRestController.java` -- Complete REST controller pattern (findAll, get, post, put, batch, json_path, validation)
- `libs/com.etendorx.generate_entities/src/main/resources/org/openbravo/base/gen/mappings/baseRestController.ftl` -- Generated controller URL pattern: `@RequestMapping("/${mappingPrefix?lower_case}/${entity.externalName}")`
- `libs/com.etendorx.generate_entities/src/main/java/com/etendorx/gen/generation/mapping/MappingGenerationBase.java` -- `mappingPrefix = projection.getName().toUpperCase()` (line 58)
- `libs/com.etendorx.das_core/src/main/java/com/etendorx/entities/mapper/lib/JsonPathEntityRetrieverBase.java` -- External ID translation pattern with `convertExternalToInternalId`
- `libs/com.etendorx.utils.auth/src/main/java/com/etendorx/utils/auth/key/context/FilterContext.java` -- JWT authentication filter (X-TOKEN header, Bearer token, AppContext population)
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/externalid/ExternalIdServiceImpl.java` -- External ID translation implementation
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/repository/DynamicRepository.java` -- Phase 3 repository with CRUD, batch, pagination
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/converter/DynamicDTOConverter.java` -- Phase 2 converter
- `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/metadata/DynamicMetadataServiceImpl.java` -- Phase 1 metadata service

### Secondary (MEDIUM confidence)
- `modules_core/com.etendorx.das/src/test/java/com/etendorx/das/test/RestCallTest.java` -- Integration test patterns showing URL format and MockMvc usage
- `modules_core/com.etendorx.das/src/test/java/com/etendorx/das/test/FieldMappingRestCallTest.java` -- Field mapping test patterns
- `libs/com.etendorx.generate_entities/src/main/resources/org/openbravo/base/gen/mappings/groupedOpenApi.ftl` -- OpenAPI configuration template

### Tertiary (LOW confidence)
- None. All findings are verified from codebase source code.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries already in build.gradle and verified in codebase
- Architecture: HIGH - Patterns directly observed from BindedRestController and generated templates
- Pitfalls: HIGH - Identified from code analysis of existing patterns and prior phase learnings
- External ID integration: MEDIUM - The exact scope of where convertExternalToInternalId should be called in the dynamic flow needs validation during implementation

**Research date:** 2026-02-06
**Valid until:** 2026-03-06 (stable, no fast-moving dependencies)
