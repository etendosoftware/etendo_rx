# Connectors: How the DAS Receives Data via etrxmapping

## Architecture Overview

The DAS (Data Access Service) exposes entities as REST endpoints through a **configuration-driven code generation** pipeline. The configuration lives in `etrx_*` tables (etrxmapping), and a Gradle task (`generate.entities`) produces all the Java artifacts the DAS needs at runtime.

```
etrxmapping tables (DB)
        |
        v
generate.entities (FreeMarker templates)
        |
        v
Generated code: Entities, Repositories, Projections, DTOs, Converters, Retrievers, Controllers
        |
        v
DAS (Spring Boot) serves REST endpoints
        |
        v
External systems connect via Connectors (InstanceConnector)
```

---

## 1. Configuration Tables (etrxmapping)

### 1.1 ETRXProjection (`etrx_projection`)

Defines a logical grouping of entity projections.

| Field         | Description                          |
|---------------|--------------------------------------|
| `name`        | Projection name                      |
| `gRPC`        | Whether to expose via gRPC           |
| `description` | Documentation                        |

### 1.2 ETRXProjectionEntity (`etrx_projection_entity`)

Maps a projection to a physical database table and controls REST exposure.

| Field                  | Description                                   |
|------------------------|-----------------------------------------------|
| `projection`           | FK to `ETRXProjection`                         |
| `tableEntity`          | FK to `AD_TABLE` (the physical table)          |
| `identity`             | Whether this is an identity entity             |
| `mappingType`          | Mapping type for the projection                |
| `restEndPoint`         | Whether to expose as a REST endpoint           |
| `externalName`         | External name used in REST API paths           |
| `createEntityMappings` | Auto-create field mappings on save             |

### 1.3 ETRXEntityField (`etrx_entity_field`)

Defines individual field mappings within a projection entity.

| Field                   | Description                                       |
|-------------------------|---------------------------------------------------|
| `name`                  | Field name in the DTO                              |
| `property`              | Source property path (dot-notation, e.g. `org.name`)|
| `ismandatory`           | Required field                                     |
| `identifiesUnivocally`  | Part of external key (used in entity retrieval)    |
| `javaMapping`           | FK to `ETRXJavaMapping` for custom logic           |
| `fieldMapping`          | Mapping type: `"DM"` = Direct Mapping              |
| `jsonpath`              | JSONPath expression for extraction from input JSON |
| `etrxConstantValue`     | FK to constant value table                         |

### 1.4 ETRXJavaMapping (`etrx_java_mapping`)

Defines custom Java-level mapping logic (converters, transformers).

| Field         | Description                                |
|---------------|--------------------------------------------|
| `name`        | Mapping name                               |
| `qualifier`   | Unique Spring bean qualifier               |
| `mappingType` | Type (`"DM"` = Direct Mapping, etc.)       |
| `table`       | FK to `AD_TABLE`                           |

### 1.5 EntityMapping (`etrx_entity_mapping`)

Links a source entity to a target entity with integration direction.

| Field                        | Description                                  |
|------------------------------|----------------------------------------------|
| `mappedEntity`               | Source entity identifier                     |
| `mappingEntity`              | Target entity identifier                     |
| `integrationDirection`       | `IN`, `OUT`, or `BOTH`                       |
| `projectionEntity`           | FK to `ETRXProjectionEntity`                 |
| `externalIdentifierRetriever`| How to match external IDs (default: `"IRU"`) |
| `smfitoOrganizationPath/Uri` | Organization context for sync                |
| `smfitoClientPath/Uri`       | Client context for sync                      |
| `smfitoDisableTriggers`      | Disable DB triggers during sync              |

---

## 2. Connector Tables

### 2.1 Connector (`etrx_connector`)

Defines an external system type.

| Field    | Description            |
|----------|------------------------|
| `name`   | Connector name         |
| `module` | Module it belongs to   |

### 2.2 InstanceConnector (`etrx_instance_connector`)

A concrete connection to an external system with credentials.

| Field               | Description                          |
|---------------------|--------------------------------------|
| `name`              | Instance name                        |
| `uRL`               | Base URL of the external system      |
| `username`          | Auth username                        |
| `password`          | Auth password                        |
| `authorizationType` | Auth mechanism                       |
| `externalEndpoint`  | Remote endpoint path                 |
| `etendoEndpoint`    | Local DAS endpoint path              |

### 2.3 InstanceConnectorMapping (`etrx_instance_mapping`)

Binds an `EntityMapping` to an `InstanceConnector`, defining what data flows between systems.

| Field                | Description                                |
|----------------------|--------------------------------------------|
| `etrxEntityMapping`  | FK to `EntityMapping` (what to sync)       |
| `instanceConnector`  | FK to `InstanceConnector` (where to sync)  |
| `filter`             | Optional filter expression                 |

---

## 3. Code Generation

### Trigger

```bash
./gradlew generate.entities
```

### FreeMarker Templates

Located in `libs/com.etendorx.generate_entities/src/main/resources/org/openbravo/base/gen/`:

| Template                              | Generates                                     |
|---------------------------------------|-----------------------------------------------|
| `entityRX.ftl`                        | JPA entity model                              |
| `jpaRepoRX.ftl`                       | Spring Data JPA repository                    |
| `datarest/jpaProjectionRX.ftl`        | Spring Data REST projection                   |
| `mappings/baseRepository.ftl`         | DAS repository (business layer)               |
| `mappings/baseDTO.ftl`                | Read/Write DTO classes                        |
| `mappings/baseDTOConverter.ftl`       | Entity <-> DTO converter                      |
| `mappings/baseJsonPathRetriever.ftl`  | Entity retriever (by JSONPath / external ID)  |
| `mappings/baseFieldConverterRead.ftl` | Read field converter                          |
| `mappings/baseFieldConverterWrite.ftl`| Write field converter                         |
| `mappings/baseRestController.ftl`     | REST controller with CRUD endpoints           |
| `mappings/baseJsonPathConverter.ftl`  | JSON input -> DTO converter                   |

### Output Directories

All generated code goes to `modules_gen/com.etendorx.entities/src/main/`:

| Directory      | Content                                          |
|----------------|--------------------------------------------------|
| `entities/`    | JPA entity models                                |
| `jparepo/`     | Spring Data JPA repositories                     |
| `projections/` | Spring Data REST projections                     |
| `mappings/`    | DTOs, converters, retrievers, controllers        |

---

## 4. Generated Artifacts (per entity)

For each configured entity/projection, the generation produces:

### 4.1 JPA Repository

```java
@RepositoryRestResource(
    excerptProjection = EntityDefaultProjection.class,
    path = "Entity_Name"
)
public interface Entity_NameRepository extends BaseDASRepository<Entity> { }
```

Extends `BaseDASRepository<T>` which provides `findAll`, `findById`, `save`, and JPA specification support.

### 4.2 Projection

```java
@Projection(name = "default", types = Entity.class)
public interface EntityDefaultProjection {
    @JsonProperty("id")
    String getId();

    @Value("#{target.getClient() != null ? target.getClient().getId() : null}")
    @JsonProperty("clientId")
    String getClientId();
}
```

Controls the shape of the JSON response. Uses SpEL expressions for computed/nested fields.

### 4.3 Read and Write DTOs

- **Read DTO** (`*DTORead`): Output model implementing `BaseDTOModel`, fields typed as `Object`.
- **Write DTO** (`*DTOWrite`): Input model implementing `BaseDTOModel`, used for POST/PUT.

### 4.4 DTO Converter

```java
@Component
public class EntityDTOConverter extends DTOConverterBase<Entity, DTORead, DTOWrite> {
    // Entity -> DTORead (for GET responses)
    public DTORead convert(Entity entity) { ... }

    // DTOWrite -> Entity (for POST/PUT requests)
    public Entity convert(DTOWrite dto, Entity existing) { ... }
}
```

### 4.5 JsonPath Retriever

```java
@Component
public class EntityJsonPathRetriever extends JsonPathEntityRetrieverBase<Entity> {
    // Retrieves entities by key fields
    // Integrates with ExternalIdService for cross-system ID resolution
}
```

Uses `identifiesUnivocally` fields from `ETRXEntityField` as lookup keys. Calls `ExternalIdService.convertExternalToInternalId()` to translate external IDs to internal Etendo IDs before querying.

### 4.6 DAS Repository (Business Layer)

```java
@Component("EntityDASRepository")
public class EntityDTORepositoryDefault
    extends BaseDTORepositoryDefault<Entity, DTORead, DTOWrite> {
    // Wires together: JPA repo + converter + retriever
}
```

### 4.7 REST Controller

```java
@RestController
@RequestMapping("/prefix/EntityName")
public class EntityRestController extends BindedRestController<DTORead, DTOWrite> {
    // GET /           -> findAll (paginated)
    // GET /{id}       -> findById
    // POST /          -> create (supports single object or JSON array)
    // PUT /{id}       -> update
    // GET /searches/* -> custom queries
}
```

### 4.8 Field Converters

- `*FieldConverterRead`: Converts entity properties to DTO values (handles type formatting, date conversion, related entity ID extraction).
- `*FieldConverterWrite`: Converts DTO values back to entity properties (handles type parsing, entity resolution).

---

## 5. Data Flow

### 5.1 Read Flow (GET)

```
GET /{prefix}/{entityName}/{id}
    |
    v
BindedRestController.get(id)
    |
    v
BaseDTORepositoryDefault.findById(id)
    |
    v
JsonPathEntityRetrieverBase.get(id)
    |-- ExternalIdService.convertExternalToInternalId(tableId, id)
    |-- JpaSpecificationExecutor.findOne(spec)
    |
    v
DTOConverter.convert(entity)  ->  DTORead
    |
    v
JSON response (via Jackson serialization)
```

### 5.2 Write Flow (POST)

```
POST /{prefix}/{entityName}
    Body: raw JSON
    |
    v
BindedRestController.post(rawJson, jsonPath)
    |-- parseJson(rawJson, jsonPath)   [supports JSONPath extraction]
    |-- Handles single object or JSON array
    |
    v
JsonPathConverter.convert(rawJson)  ->  DTOWrite
    |
    v
Validator.validate(dtoWrite)
    |
    v
BaseDTORepositoryDefault.save(dtoWrite)
    |
    v
RestCallTransactionHandler.begin()
    |
    v
JsonPathRetriever.get(id)  [check for existing entity / upsert]
    |
    v
DTOConverter.convert(dtoWrite, existingEntity)
    |-- FieldConverterWrite (per field)
    |-- ExternalIdService (resolve foreign keys)
    |-- MappingUtils (date parsing, type conversion)
    |
    v
Validator.validate(entity)  [JPA constraint validation]
    |
    v
BaseDASRepository.save(entity)  ->  Hibernate  ->  DB
    |
    v
ExternalIdService.add(tableId, externalId, entity)
ExternalIdService.flush()  [stores ID mapping in etrx_instance_externalid]
    |
    v
RestCallTransactionHandler.commit()
    |
    v
DTOConverter.convert(savedEntity)  ->  DTORead response
```

### 5.3 Connector Sync Flow

```
External System
    |
    v
InstanceConnector (URL, Auth credentials)
    |
    v
InstanceConnectorMapping
    |-- EntityMapping (field-level mapping rules)
    |-- filter (optional query filter)
    |
    v
OBCONFieldMapping.map(instanceConnectorMapping)
    |-- Iterates ETRXEntityField list from ProjectionEntity
    |-- Extracts: name, jsonpath, fieldMapping, isExternalIdentifier
    |-- Resolves related entity metadata (ad_table_id, entityName)
    |
    v
DAS REST endpoint (POST with mapped JSON)
    |
    v
JsonPathConverter  ->  DTOWrite  ->  Entity  ->  DB
    |
    v
ExternalIdService stores ID mapping for future lookups
```

---

## 6. Core Interfaces

Located in `libs/com.etendorx.das_core/src/main/java/com/etendorx/entities/mapper/lib/`:

| Interface/Class                 | Responsibility                                              |
|---------------------------------|-------------------------------------------------------------|
| `DASRepository<E, F>`           | CRUD operations: `findAll`, `findById`, `save`, `update`    |
| `DTOConverter<E, F, G>`         | Entity <-> DTO conversion                                   |
| `DTOConverterBase<E, F, G>`     | Base impl with `Iterable` and `Page` conversion             |
| `JsonPathEntityRetriever<E>`    | Retrieve entity by key values                               |
| `JsonPathEntityRetrieverBase<E>`| Base impl using JPA Specifications + ExternalIdService       |
| `JsonPathConverter<F>`          | Raw JSON string -> DTO conversion                           |
| `ExternalIdService`             | Cross-system ID resolution and storage                      |
| `BindedRestController<E, F>`    | Base REST controller with GET/POST/PUT + JSONPath support   |
| `BaseDTOModel`                  | Marker interface for DTOs (provides `getId`/`setId`)        |
| `BaseDASRepository<T>`          | Spring Data JPA base repository                             |

Located in `modules_gen/com.etendorx.entities/src/main/entities/`:

| Class                           | Responsibility                                              |
|---------------------------------|-------------------------------------------------------------|
| `BaseDTORepositoryDefault<T,E,F>` | Orchestrates save/update: transaction, conversion, validation, external ID mapping |

---

## 7. External ID Resolution

The `ExternalIdService` is central to connector integrations:

1. **On write**: After saving an entity, `ExternalIdService.add(tableId, externalId, entity)` queues an ID mapping. `flush()` persists it to `etrx_instance_externalid`.

2. **On read/resolve**: `convertExternalToInternalId(tableId, externalId)` looks up the internal Etendo ID from `etrx_instance_externalid` using the current user's `externalSystemId` context.

3. **Fallback**: If no mapping is found, the external ID is returned as-is (treated as an internal ID).

This allows connectors to use their own IDs when pushing data, while Etendo maintains a bidirectional mapping table.

---

## 8. OBCONFieldMapping (Connector Field Mapper)

`OBCONFieldMapping` (`modules_core/com.etendorx.das/src/main/java/com/etendorx/das/connector/OBCONFieldMapping.java`) maps connector configuration to a list of field descriptors:

For each `ETRXEntityField` in the projection:

```json
{
  "name": "fieldName",
  "jsonpath": "$.fieldName",
  "fieldMapping": "DM",
  "isExternalIdentifier": true/false,
  "ad_table_id": "...",
  "entityName": "...",
  "isArray": true/false
}
```

It resolves property paths through metadata to identify related entities and array properties, enabling the connector to know how to extract and map each field from the external system's JSON payload.

---

## 9. DAS Application Bootstrap

`EtendorxDasApplication` (`modules_core/com.etendorx.das/src/main/java/com/etendorx/das/EtendorxDasApplication.java`) scans:

| Package                          | Purpose                         |
|----------------------------------|---------------------------------|
| `com.etendorx.entities.jparepo`  | JPA repositories (via `@EnableJpaRepositories`) |
| `com.etendorx.entities.mappings` | Generated DTOs, converters, controllers |
| `com.etendorx.entities.metadata` | Entity metadata services        |
| `com.etendorx.das`               | DAS core services               |
| `com.etendorx.das.externalid`    | External ID service             |
| `com.etendorx.utils.auth.key`    | Authentication                  |
| `com.etendorx.defaultvalues`     | Default value handlers          |

---

## 10. Key Design Patterns

| Pattern                    | Description                                                      |
|----------------------------|------------------------------------------------------------------|
| **Code Generation**        | FreeMarker templates produce boilerplate from DB configuration   |
| **DTO Separation**         | Read/Write DTOs decouple API contract from JPA entity model      |
| **JsonPath Extraction**    | Flexible field extraction from arbitrary JSON structures          |
| **External ID Mapping**    | Bidirectional ID translation between Etendo and external systems |
| **Upsert by Default**      | POST checks for existing entity via retriever before insert      |
| **Transactional Boundary** | `RestCallTransactionHandler` wraps save + external ID flush      |
| **Spring Component Wiring**| Each mapping generates `@Component` beans, Spring auto-wires     |

---

## 11. File Locations Summary

| Purpose                     | Path                                                                        |
|-----------------------------|-----------------------------------------------------------------------------|
| etrxmapping entity models   | `modules_gen/com.etendorx.entities/src/main/entities/com/etendoerp/etendorx/data/` |
| Generated JPA repositories  | `modules_gen/com.etendorx.entities/src/main/jparepo/`                       |
| Generated projections       | `modules_gen/com.etendorx.entities/src/main/projections/`                   |
| Generated mappings (DTOs, etc.) | `modules_gen/com.etendorx.entities/src/main/mappings/`                  |
| DAS core library            | `libs/com.etendorx.das_core/src/main/java/com/etendorx/entities/mapper/lib/` |
| DAS application             | `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/`            |
| Code generation engine      | `libs/com.etendorx.generate_entities/`                                      |
| FreeMarker templates        | `libs/com.etendorx.generate_entities/src/main/resources/org/openbravo/base/gen/` |
| Connector field mapping     | `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/connector/`  |
| External ID service         | `modules_core/com.etendorx.das/src/main/java/com/etendorx/das/externalid/` |
