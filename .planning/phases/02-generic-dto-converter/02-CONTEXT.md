# Phase 2: Generic DTO Converter - Context

**Gathered:** 2026-02-06
**Status:** Ready for planning

<domain>
## Phase Boundary

Convert between JPA entities and `Map<String, Object>` using runtime metadata from Phase 1. Bidirectional: Entity→Map for reads, Map→Entity for writes. This converter replaces the generated `*DTOConverter` classes with a single dynamic implementation.

</domain>

<decisions>
## Implementation Decisions

### Portability Strategy
- **Develop fully in RX, document for Classic reimplementation**
- Java code will NOT be shared directly between RX and Classic (incompatible: Hibernate 6/jakarta vs Hibernate 5/javax, EntityManager vs OBDal, Spring DI vs singletons)
- The **data structure** (etrx_* tables) is 100% shared — same tables, same schema
- The **conversion algorithm** (how to walk fields, apply mappings, resolve relations) will be well-documented so Classic can reimplement using OBDal
- Accept code duplication between platforms; prioritize working RX implementation

### Property Access
- Use **Hibernate PropertyAccessor/Getter** to read/write entity properties
- Aligned with what Hibernate uses internally for entity access
- In Classic reimplementation, this would become `BaseOBObject.getValue()`/`setValue()`

### Entity Mappings (EM) - Read Direction
- **Nested object completo**: When reading an EM field, recursively convert the related entity and return the full sub-object in the Map
- Not just ID+identifier — the complete related entity as a nested Map

### Entity References - Write Direction
- **Resolve by externalId**: When writing, entity references in the Map are resolved via the ExternalId system (connector integration)
- This aligns with the connector use case where external systems provide their own IDs

### Type Coercion
- **Compatible with current generated converters**: Replicate `MappingUtils.handleBaseObject()` behavior
- Same date formats, same decimal precision, same null handling as existing generated code
- This ensures API response format remains identical

### Audit Fields
- **Replicate current generated converter behavior** for createdBy, creationDate, updatedBy, updated
- Research needed: examine generated converters to determine exact audit field handling

### Null Handling
- **Replicate current `MappingUtils.handleBaseObject()` behavior** for null values
- Research needed: examine MappingUtils to determine if null = clear vs null = skip

### Mandatory Field Validation
- **Claude's discretion** on validation mechanism
- Must return clear error when mandatory fields are missing on write

### Java Mappings (JM)
- **Replicate current generated converter pattern** for resolving custom mapping classes by qualifier
- Research needed: examine `baseDTOConverter.ftl` to understand how JM qualifiers are resolved to Spring beans

</decisions>

<specifics>
## Specific Ideas

- The converter must produce identical JSON output to what the generated `*DTOConverter` classes produce today — this ensures existing API consumers aren't affected
- EM fields with nested objects mean the converter must handle recursion and cycle detection
- ExternalId resolution on writes leverages existing `ExternalIdService` infrastructure
- When porting to Classic later, `OBDal.getInstance().get(Class, id)` replaces `EntityManager.find()`, and `BaseOBObject.getValue(propName)` replaces Hibernate PropertyAccessor

</specifics>

<deferred>
## Deferred Ideas

- Shared Java module between RX and Classic — not viable due to Hibernate version incompatibility
- Upgrading Classic to Hibernate 6 / Jakarta — would enable code sharing but is a massive undertaking
- Abstract interface layer (MetadataAccess, DTOConverter interfaces) — could be revisited if Classic port becomes a priority

</deferred>

---

*Phase: 02-generic-dto-converter*
*Context gathered: 2026-02-06*
